package com.glip.driver.map

object RouteCoordinateResolver {

    /**
     * Resolves human-readable place names to accurate GPS coordinates.
     * Maps local corridors (Motipur, Baruraj, Bus Stand, Railway Station, Muzaffarpur, etc.).
     */
    fun resolveCoordinates(
        placeName: String,
        fallbackLat: Double = 26.1739,
        fallbackLng: Double = 85.1818
    ): Pair<Double, Double> {
        val q = placeName.lowercase().trim()
        return when {
            // Home / Baruraj
            q.contains("home") || q.contains("ghar") || q.contains("baruraj") -> Pair(26.1465, 85.0782)

            // Railway Station
            q.contains("rail") || q.contains("train") || (q.contains("motipur") && q.contains("station")) -> Pair(26.1770, 85.1865)

            // Bus Stand
            q.contains("bairia") || (q.contains("muzaffarpur") && q.contains("bus")) -> Pair(26.1340, 85.3580)
            q.contains("bus") || q.contains("stand") || q.contains("stop") -> Pair(26.1755, 85.1830)

            // Motipur Local
            q.contains("motipur") -> Pair(26.1739, 85.1818)

            // Muzaffarpur
            q.contains("muzaffarpur") -> Pair(26.1209, 85.3647)

            // Tajpur Mor / Bypass
            q.contains("tajpur") -> Pair(26.1600, 85.1300)

            // Kanti
            q.contains("kanti") -> Pair(26.1980, 85.2950)

            // Sahebganj
            q.contains("sahebganj") -> Pair(26.2570, 85.0210)

            // Patna
            q.contains("patna") -> Pair(25.5941, 85.1376)

            // Chhattisgarh (Rajnandgaon / Dongargarh Corridor)
            q.contains("rajnandgaon") -> Pair(21.0974, 81.0336)
            q.contains("dongargarh") -> Pair(21.1873, 80.7618)
            q.contains("nava gaon") || q.contains("patel") -> Pair(21.1200, 80.9500)

            // Delhi / NCR
            q.contains("connaught") || q.contains("delhi") -> Pair(28.6139, 77.2090)
            q.contains("noida") -> Pair(28.5355, 77.3910)

            fallbackLat != 0.0 && fallbackLng != 0.0 -> Pair(fallbackLat, fallbackLng)
            else -> Pair(26.1739, 85.1818)
        }
    }
}
