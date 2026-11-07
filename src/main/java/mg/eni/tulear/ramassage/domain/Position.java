package mg.eni.tulear.ramassage.domain;

/** Point GPS (degrés décimaux). */
public record Position(double latitude, double longitude) {

    private static final double RAYON_TERRE_KM = 6371.0;

    /** Distance à vol d'oiseau en km (formule de haversine). */
    public double distanceVers(Position autre) {
        double dLat = Math.toRadians(autre.latitude - latitude);
        double dLon = Math.toRadians(autre.longitude - longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(autre.latitude))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * RAYON_TERRE_KM * Math.asin(Math.sqrt(a));
    }
}
