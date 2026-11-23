package mg.eni.tulear.ramassage.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Génère des mots de passe temporaires aléatoires (sans caractères ambigus : 0/O, 1/l/I). */
@Component
public class GenerateurMotDePasse {

    static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    static final int LONGUEUR = 12;

    private final SecureRandom aleatoire = new SecureRandom();

    public String generer() {
        StringBuilder sb = new StringBuilder(LONGUEUR);
        for (int i = 0; i < LONGUEUR; i++) {
            sb.append(ALPHABET.charAt(aleatoire.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
