package cl.oylfrutossecos.clientes.dto;

public record LoginResponse(

        String token,

        String tipo,

        long expiraEn,

        ClienteResponse cliente

) {
}