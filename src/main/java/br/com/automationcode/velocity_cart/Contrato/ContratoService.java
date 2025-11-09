package br.com.automationcode.velocity_cart.Contrato;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.automationcode.velocity_cart.Aluguel.Aluguel;
import br.com.automationcode.velocity_cart.Aluguel.AluguelService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContratoService {

    @Autowired
    AluguelService aluguelService;

    private final ContratoRepository contratoRepository;

    @Transactional
    public Contrato salvarContrato(Contrato contrato) {
        // Vincula os itens ao contrato
        contrato.getItens().forEach(item -> item.setContrato(contrato));

        // Calcula o valor total antes de salvar
        contrato.calcularValorTotal();

        return contratoRepository.save(contrato);
    }

    public List<Contrato> listarContratos() {

        List<Contrato> contratos = contratoRepository.findAll();

        List<Contrato> ordenados = contratos.stream()
                .sorted(Comparator
                        .comparingLong(c -> Math.abs(ChronoUnit.DAYS.between(c.getDataInicio(), LocalDate.now()))))
                .collect(Collectors.toList());

        ordenados = ordenados.stream()
                .filter(contrato -> !contrato.isFinalizado())
                .collect(Collectors.toList());

        return ordenados;
    }

    public List<Contrato> listarContratosFinalizados() {
        List<Contrato> contratos = contratoRepository.findAll();
        return contratos.stream()
                .filter(Contrato::isFinalizado)
                .collect(Collectors.toList());
    }

    public Contrato buscarPorId(Long id) {
        return contratoRepository.findById(id).orElseThrow(() -> new RuntimeException("Contrato não encontrado"));
    }

    @Transactional
    public void deletar(Long id) {
        contratoRepository.deleteById(id);
    }

    public Contrato iniciarContrato(Long contratoId) {
        Contrato contrato = buscarPorId(contratoId);
        contrato.setFinalizado(true);
        salvarContrato(contrato);

        contrato.getItens().forEach(item -> {
            Aluguel aluguel = new Aluguel();
            aluguel.setNomeResponsavel(contrato.getNomeContratante());
            aluguel.setTempoEscolhido(item.getTempoUso());
            aluguel.setProduto(item.getProduto());
            aluguel.setAtendente("Contrato " + contrato.getNomeContratante());
            aluguel.setFormaPagamento("Contrato");
            aluguel.setPago(true);

            aluguelService.criarAluguel(aluguel);
        });

        return contrato;
    }
}