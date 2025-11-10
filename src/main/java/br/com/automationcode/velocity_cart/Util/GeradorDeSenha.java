package br.com.automationcode.velocity_cart.Util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GeradorDeSenha {

    public static void main(String[] args) {
        String senha = "velocity";
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String senhaCriptografada = encoder.encode(senha);
        System.out.println("Senha criptografada: " + senhaCriptografada);

        //System.out.println(verificarSenha("laercio", "$2a$10$X1yK/AH7ywVo1ryiw/zM/uVUpIwHK65jt8xP1yh8bFF3dUhEynA9W"));
        }

    // Não é possível descobrir a senha original a partir da senha criptografada gerada pelo BCryptPasswordEncoder.
    // O BCrypt é um algoritmo de hash unidirecional, projetado para proteger senhas.
    // Você pode apenas verificar se uma senha corresponde ao hash usando o método verificarSenha abaixo.

    public static boolean verificarSenha(String senha, String senhaCriptografada) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.matches(senha, senhaCriptografada);
    }
}
