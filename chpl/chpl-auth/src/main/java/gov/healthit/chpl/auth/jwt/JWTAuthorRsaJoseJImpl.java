package gov.healthit.chpl.auth.jwt;



import gov.healthit.chpl.auth.AuthPropertiesConsumer;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class JWTAuthorRsaJoseJImpl extends AuthPropertiesConsumer implements JWTAuthor {
	
	@Autowired
	@Qualifier("RsaJose4JWebKey")
	JSONWebKey jwk;
	
	
	Logger logger = LogManager.getLogger(JWTAuthorRsaJoseJImpl.class.getName());
	
	
	@Override
	public String createJWT(String subject, Map<String, List<String> > claims) {
		
		Properties jwtProps = this.getProps();
		
	    // Create the Claims, which will be the content of the JWT
	    JwtClaims claimsObj = new JwtClaims();
	    claimsObj.setIssuer(jwtProps.getProperty("jwtIssuer"));  // who creates the token and signs it
	    claimsObj.setAudience(jwtProps.getProperty("jwtAudience")); // to whom the token is intended to be sent
	    claimsObj.setExpirationTimeMinutesInTheFuture(
	    		Integer.parseInt(jwtProps.getProperty("jwtExpirationTimeMinutesInTheFuture"))); // time when the token will expire (from now)
	    claimsObj.setGeneratedJwtId(); // a unique identifier for the token
	    claimsObj.setIssuedAtToNow();  // when the token was issued/created (now)
	    claimsObj.setNotBeforeMinutesInThePast(Integer.parseInt(
	    		jwtProps.getProperty("jwtNotBeforeMinutesInThePast"))); // time before which the token is not yet valid (minutes ago)
	    claimsObj.setSubject(subject); // the subject/principal is whom the token is about
	    
	    
	    for (Map.Entry<String, List<String> > claim : claims.entrySet())
	    {
	    	claimsObj.setStringListClaim(claim.getKey(), claim.getValue());	
	    }
	    
	    // A JWT is a JWS and/or a JWE with JSON claims as the payload.
	    // In this example it is a JWS so we create a JsonWebSignature object.
	    JsonWebSignature jws = new JsonWebSignature();

	    // The payload of the JWS is JSON content of the JWT Claims
	    jws.setPayload(claimsObj.toJson());

	    // The JWT is signed using the private key
	    jws.setKey(jwk.getPrivateKey());

	    // Set the Key ID (kid) header because it's just the polite thing to do.
	    // We only have one key in this example but a using a Key ID helps
	    // facilitate a smooth key rollover process
	    //jws.setKeyIdHeaderValue(rsaJsonWebKey.getKeyId());

	    // Set the signature algorithm on the JWT/JWS that will integrity protect the claims
	    jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

	    // Sign the JWS and produce the compact serialization or the complete JWT/JWS
	    // representation, which is a string consisting of three dot ('.') separated
	    // base64url-encoded parts in the form Header.Payload.Signature
	    // If you wanted to encrypt it, you can simply set this jwt as the payload
	    // of a JsonWebEncryption object and set the cty (Content Type) header to "jwt".
	    String jwt = null;
		try {
			jwt = jws.getCompactSerialization();
		} catch (JoseException e) {
			logger.error("Token creation error", e);
		}
	    
	    return jwt;
	}
}
