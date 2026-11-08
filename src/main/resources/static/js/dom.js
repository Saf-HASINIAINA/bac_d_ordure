/** Crée un élément DOM sans jamais interpréter du HTML : les données restent du texte (pas d'injection). */
export function h(balise, proprietes = {}, ...enfants) {
  const element = document.createElement(balise);
  for (const [cle, valeur] of Object.entries(proprietes)) {
    if (valeur === false || valeur == null) continue;
    if (cle === 'class') element.className = valeur;
    else if (cle.startsWith('on')) element.addEventListener(cle.slice(2), valeur);
    else element.setAttribute(cle, valeur === true ? '' : valeur);
  }
  for (const enfant of enfants.flat()) {
    if (enfant != null && enfant !== false) element.append(enfant);
  }
  return element;
}

export function remplacer(conteneur, ...enfants) {
  conteneur.replaceChildren(...enfants.flat().filter((e) => e != null && e !== false));
}

const formatNombre = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 });
export const entier = (n) => formatNombre.format(n);

export function depuis(iso) {
  const secondes = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 1000));
  if (secondes < 60) return `il y a ${secondes} s`;
  if (secondes < 3600) return `il y a ${Math.round(secondes / 60)} min`;
  return `il y a ${Math.round(secondes / 3600)} h`;
}

const formatDecimal = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 1 });
export const decimal = (n) => formatDecimal.format(n);

/** Date et heure locales lisibles (ex. « 07/11/2026 14:05 »). */
export function dateHeure(iso) {
  if (!iso) return '–';
  return new Date(iso).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' });
}

/** Ajoute des éléments à un conteneur en ignorant null, undefined et false (append(null) écrirait le texte « null »). */
export function insererDans(conteneur, ...enfants) {
  conteneur.append(...enfants.flat().filter((e) => e != null && e !== false));
}
