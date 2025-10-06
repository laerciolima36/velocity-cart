package br.com.automationcode.velocity_cart.Contrato;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContratoService {

    private final ContratoRepository contratoRepository;

    @Transactional
    public Contrato salvarContrato(Contrato contrato) {
        contrato.getItens().forEach(item -> item.setContrato(contrato));
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
}
