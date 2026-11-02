package com.bulon.stock.auth;

import com.bulon.stock.usuarios.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Emite el token de sesión. Es un JWT firmado con la clave del servidor: el cliente lo puede
 * leer pero no modificar (si cambia el rol, la firma deja de coincidir y el token se rechaza).
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    static final String CLAIM_USUARIO_ID = "uid";
    static final String CLAIM_SUCURSAL_ID = "sucursalId";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties props;

    public Token generar(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(props.expiracionHoras(), ChronoUnit.HOURS);
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("bulon-stock")
                .subject(usuario.getUsername())
                .issuedAt(ahora)
                .expiresAt(expira)
                .claim(CLAIM_USUARIO_ID, usuario.getId())
                .claim(SecurityConfig.CLAIM_ROL, usuario.getRol().name());
        if (usuario.getSucursal() != null) {
            claims.claim(CLAIM_SUCURSAL_ID, usuario.getSucursal().getId());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new Token(valor, expira);
    }

    /** Id del usuario logueado, leído del token del request. */
    public static Long usuarioId(Jwt jwt) {
        Object uid = jwt.getClaim(CLAIM_USUARIO_ID);
        return uid instanceof Number n ? n.longValue() : Long.valueOf(uid.toString());
    }

    public record Token(String valor, Instant expira) {
    }
}
