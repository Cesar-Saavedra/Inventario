package cl.duoc.ms_inventario.security;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
@Component
public class JwtUtil {

    // Clave secreta compartida con ms-login (se lee desde application.properties)
    @Value("${jwt.secret}")
    private String secret;

    // Convierte el texto del secret en una clave criptografica que jjwt entiende
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /*
     * Extrae todos los datos (claims) del token.
     * Lanza una excepcion si el token es invalido o esta vencido.
     */
    public Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseClaimsJws(token)
                .getPayload();
    }

    /*
     * Devuelve true si el token tiene buena firma y no esta vencido.
     * Devuelve false si el token es invalido, fue alterado o vencio.
     */
    public boolean esTokenValido(String token) {
        try {
            extraerClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Metodos para sacar datos especificos del token sin leerlo completo

    // Id del usuario autenticado (Integer, igual que en ms-login)
    public Integer extraerId(String token) {
        return extraerClaims(token).get("id", Integer.class);
    }

    // Nombre del usuario autenticado
    public String extraerNombre(String token) {
        return extraerClaims(token).get("nombre", String.class);
    }

    // Rol del usuario: JUGADOR, TIENDA u ORGANIZADOR
    public String extraerRol(String token) {
        return extraerClaims(token).get("rol", String.class);
    }

    /*
     * Saca el token limpio del header Authorization.
     *
     * El header llega como: "Bearer eyJhbGci..."
     * Este metodo devuelve solo: "eyJhbGci..."
     *
     * Devuelve null si el header no tiene el formato correcto.
     */
    public String obtenerTokenDelHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

}
