package eu.h2020_5gtango.vnv.tee.restclient

import eu.h2020_5gtango.vnv.tee.model.Session
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class UserSessionManager {

    @Autowired
    @Qualifier('restTemplateWithoutAuth')
    RestTemplate restTemplate

    @Value('${app.gk.session.endpoint}')
    def sessionEndpoint

    @Value('${app.gk.session.username}')
    def username

    @Value('${app.gk.session.password}')
    def password

    Session session

    synchronized String retrieveValidBearerToken() {
        if (session == null || session.invalid()) {
            session = restTemplate.postForEntity(sessionEndpoint, [username: username, password: password], Session.class).body
        }
        session.token.access_token
    }
}
