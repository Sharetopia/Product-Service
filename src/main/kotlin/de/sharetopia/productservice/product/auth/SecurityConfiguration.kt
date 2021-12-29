package de.sharetopia.productservice.product.auth

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfiguration : WebSecurityConfigurerAdapter() {
    @Autowired
    private lateinit var awsCognitoJwtAuthenticationFilter: AwsCognitoJwtAuthFilter

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http.headers().cacheControl()
        http.cors()
        http.csrf().disable()
            .authorizeRequests()
            .antMatchers("**/health").permitAll()
            .antMatchers("/api/**").authenticated()
            .and()
            .addFilterBefore(awsCognitoJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
    }
}