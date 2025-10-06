package br.com.automationcode.velocity_cart.Produto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private static final String UPLOAD_DIR = "imagens/produtos/";

    public List<Produto> listarTodos() {
        return produtoRepository.findAllByOrderByNomeAsc();
    }

    public Produto buscarPorId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID do produto inválido.");
        }

        return produtoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado!"));
    }

    public Produto salvar(Produto produto) {
        if (produto.getCodigo() == null || produto.getCodigo().isEmpty()) {
            throw new IllegalArgumentException("O código do produto não pode ser nulo ou vazio.");
        }

        if (produtoRepository.existsByCodigo(produto.getCodigo())) {
            throw new IllegalArgumentException("Já existe um produto com o código: " + produto.getCodigo());
        }
        return produtoRepository.save(produto);
    }

    public Produto atualizar(Long id, Produto produtoAtualizado) {
        Produto estoque = buscarPorId(id);

        estoque.setNome(produtoAtualizado.getNome());
        estoque.setCodigo(produtoAtualizado.getCodigo());
        estoque.setFotoProduto(produtoAtualizado.getFotoProduto());
        estoque.setQuantidadeEstoque(produtoAtualizado.getQuantidadeEstoque());
        estoque.setPrecoCusto(produtoAtualizado.getPrecoCusto());
        estoque.setPrecoVenda(produtoAtualizado.getPrecoVenda());

        return produtoRepository.save(estoque);
    }

    public void deletar(Long id) {
        Produto produto = buscarPorId(id);
        produtoRepository.delete(produto);
    }

    public ResponseEntity<String> salvarFotoById(MultipartFile file, Long id) {
        try {
            // Gera um nome único para a imagem
            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path path = Paths.get(UPLOAD_DIR + filename);

            // Cria a pasta se não existir
            Files.createDirectories(path.getParent());

            // Salva o arquivo no sistema de arquivos
            Files.write(path, file.getBytes());

            // Cria e salva o produto no banco
            Produto produto = buscarPorId(id);
            produto.setFotoProduto(filename);

            produtoRepository.save(produto);

            return ResponseEntity.ok("Imagem salva com sucesso: " + filename);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao salvar imagem");
        }
    }

    public void aumentarQuantidadeEstoque(Long id, int quantidade) {
        Produto produto = buscarPorId(id);
        int novaQuantidade = produto.getQuantidadeEstoque() + quantidade;
        produto.setQuantidadeEstoque(novaQuantidade);
        produtoRepository.save(produto);
    }

    public void diminuirQuantidadeEstoque(Long id, int quantidade) {
        Produto produto = buscarPorId(id);
        int novaQuantidade = produto.getQuantidadeEstoque() - quantidade;
        produto.setQuantidadeEstoque(novaQuantidade);
        produtoRepository.save(produto);
    }

    public void verificarEstoqueDisponivel(Produto produto, int quantidadeNaMaquina) {
        if (produto.getQuantidadeEstoque() < quantidadeNaMaquina) {
            throw new IllegalArgumentException(
                    "A quantidade que voçê deseja inserir na maquina é maior que o estoque do produto.");
        }
    }

}