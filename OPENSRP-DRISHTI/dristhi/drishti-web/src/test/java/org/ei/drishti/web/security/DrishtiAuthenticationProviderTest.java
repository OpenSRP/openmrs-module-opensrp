package org.ei.drishti.web.security;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.UUID;

import org.ei.drishti.domain.DrishtiUser;
import org.ei.drishti.repository.AllDrishtiUsers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.google.gson.Gson;

public class DrishtiAuthenticationProviderTest {
    @Mock
    private AllDrishtiUsers allDrishtiUsers;
    @Mock
    private ShaPasswordEncoder passwordEncoder;
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private DrishtiAuthenticationProvider authenticationProvider;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        authenticationProvider = new DrishtiAuthenticationProvider(allDrishtiUsers, passwordEncoder);
    }

    @Test
    public void shouldAuthenticateValidUser() throws Exception {
        when(allDrishtiUsers.findByUsername("user 1")).thenReturn(new DrishtiUser("user 1", "hashed password 1", "salt", asList("ROLE_USER", "ROLE_ADMIN"), true));
        when(passwordEncoder.encodePassword("password 1", "salt")).thenReturn("hashed password 1");

        Authentication authentication = authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("user 1", "password 1"));

        assertEquals(new UsernamePasswordAuthenticationToken("user 1", "password 1", asList(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))), authentication);
    }

    @Test
    public void shouldNotAuthenticateUserWithWrongUsername() throws Exception {
        when(allDrishtiUsers.findByUsername("user 1")).thenReturn(null);
        exception.expect(BadCredentialsException.class);
        exception.expectMessage("The username or password you entered is incorrect. Please enter the correct credentials.");

        authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("user 1", "password 1"));
    }

    @Test
    public void shouldNotAuthenticateUserWithWrongPassword() throws Exception {
        when(allDrishtiUsers.findByUsername("user 1")).thenReturn(new DrishtiUser("user 1", "correct password", "salt", asList("ROLE_USER"), true));
        exception.expect(BadCredentialsException.class);
        exception.expectMessage("The username or password you entered is incorrect. Please enter the correct credentials.");

        authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("user 1", "wrong password"));
    }

    @Test
    public void shouldNotAuthenticateInactiveUser() throws Exception {
        when(allDrishtiUsers.findByUsername("user 1")).thenReturn(new DrishtiUser("user 1", "hashed password 1", "salt", asList("ROLE_USER"), false));
        when(passwordEncoder.encodePassword("password 1", "salt")).thenReturn("hashed password 1");
        exception.expect(BadCredentialsException.class);
        exception.expectMessage("The user has been registered but not activated. Please contact your local administrator.");

        authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("user 1", "password 1"));
    }

    @Test
    public void shouldFetchDrishtiUserByUsername() throws Exception {
        authenticationProvider.getDrishtiUser("user 1");

        verify(allDrishtiUsers).findByUsername("user 1");
    }

    @Test
    /*@Ignore*/
    public void toGenerateUserPasswordsAndSalt() throws Exception {
        String username = "lhshavalian";
        String password = "lhsh123456";
        UUID salt = randomUUID();
        String hashedPassword = new ShaPasswordEncoder().encodePassword(password, salt);
        System.out.println(new Gson().toJson(new DrishtiUser(username, hashedPassword, salt.toString(), asList("ROLE_USER"), true)));
    }
    
    public void toGenerateUserPasswordsAndSalt(String username,String password) throws Exception {
        UUID salt = randomUUID();
        String hashedPassword = new ShaPasswordEncoder().encodePassword(password, salt);
        System.out.println(new Gson().toJson(new DrishtiUser(username, hashedPassword, salt.toString(), asList("ROLE_USER"), true)));
    }
    
    public static void main(String[] args) throws Exception {
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("demo1","demo1321");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("testuser","test321");

    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("lhwsadcabt","lhwabt123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("lhslora","lhsora123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("lhsjhangi","lhsngi123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("lhsbaldhari","lhsari123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("lhshavalianvill","lhsill123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("lhshavalianurban","lhsban123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("lhsjhangra","lhsgra123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("lhstajwal","lhswal123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("lhsnagribala","lhsala123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("lhsmajuhan","lhshan123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("lhslangra","lhsgra123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("lhsgariphulgran","lhsran123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("phchajiagali","phcali123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("phcseergharbi","phcrbi123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("phcsumagarakha","phckha123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("phcseersharqi","phcrqi123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("phcdewal","phcwal123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("phchavalian","phcian123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("phclora","phcora123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("distkhatibatd","disatd123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("khatibhavalian","khaian123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("lgucsechavalian","lguian123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("lgucsectajwal","lguwal123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("lgucsecabturban","lguban123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("lgucsecmirpur","lgupur123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("lgucseclora","lguora123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("patucmirpur1","patur1123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("patucmirpur2","patur2123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("patuchaval1","patal1123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("patuchaval2","patal2123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("phcwch","phcwch123");
    	new DrishtiAuthenticationProviderTest().toGenerateUserPasswordsAndSalt("phcath","phcath123");

	}
}
