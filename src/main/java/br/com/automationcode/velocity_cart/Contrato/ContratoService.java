package br.com.automationcode.velocity_cart.Contrato;

import java.util.List;

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
        return contratoRepository.findAll();
    }

    public Contrato buscarPorId(Long id) {
        return contratoRepository.findById(id).orElseThrow(() -> new RuntimeException("Contrato não encontrado"));
    }

    @Transactional
    public void deletar(Long id) {
        contratoRepository.deleteById(id);
    }

    public Contrato iniciarContrato(Long contratoId){
        Contrato contrato = buscarPorId(contratoId);

        contrato.getItens().forEach(item -> {
            Aluguel aluguel = new Aluguel();
            aluguel.setNomeResponsavel(contrato.getNomeContratante());
            aluguel.setNomeCrianca("Contrato: " + contrato.getNumeroContrato());
            aluguel.setTempoEscolhido(item.getTempoUso());
            aluguel.setProduto(item.getProduto());
            
            aluguelService.criarAluguel(aluguel);
        });

        return contrato;
    }
}