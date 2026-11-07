package mg.eni.tulear.ramassage.api.dto;

import mg.eni.tulear.ramassage.domain.Chauffeur;

public record ChauffeurDto(Long id, String nom, String prenom, String numeroPermis) {
    public static ChauffeurDto from(Chauffeur c) {
        return new ChauffeurDto(c.getId(), c.getNom(), c.getPrenom(), c.getNumeroPermis());
    }
}
