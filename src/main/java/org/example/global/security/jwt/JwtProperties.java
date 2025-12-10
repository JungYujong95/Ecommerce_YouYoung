package org.example.global.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private String issuer;
    private Access access = new Access();
    private Refresh refresh = new Refresh();

    @Getter
    @Setter
    public static class Access {
        private long expiration = 3600000;
        private String header = "Authorization";
        private String prefix = "Bearer ";
    }

    @Getter
    @Setter
    public static class Refresh {
        private long expiration = 1209600000;
    }
}
