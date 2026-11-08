/** État partagé par toutes les pages : utilisateur connecté et tournée choisie pour la carte. */
export const etat = {
  moi: null,
  tourneeChoisie: null, // tournée dont le trajet est tracé sur la carte (null = première tournée active)
  configuration: null, // position du dépôt
};

export const estAdmin = () => etat.moi?.role === 'ADMIN';
export const estChauffeur = () => etat.moi?.role === 'CHAUFFEUR';
/** Administrateurs et superviseurs exploitent ; un chauffeur ne fait que dérouler ses tournées. */
export const peutGerer = () => estAdmin() || etat.moi?.role === 'SUPERVISEUR';
