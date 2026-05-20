package com.example.vibemeet.services;

import com.example.vibemeet.models.Venue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VenueService {

    public static final double CASABLANCA_LAT = 33.5731;
    public static final double CASABLANCA_LNG = -7.5898;

    private static List<Venue> cachedVenues = null;

    public List<Venue> getNearbyVenues(double userLat, double userLon, double radiusKm) {
        List<Venue> allVenues = getAllVenuesInCasablanca();
        List<Venue> nearbyVenues = new ArrayList<>();

        for (Venue venue : allVenues) {
            double distance = calculateDistance(userLat, userLon, venue.getLatitude(), venue.getLongitude());
            if (distance <= radiusKm) {
                venue.setDistanceKm(distance);
                nearbyVenues.add(venue);
            }
        }

        nearbyVenues.sort((v1, v2) -> Double.compare(v1.getDistanceKm(), v2.getDistanceKm()));
        return nearbyVenues;
    }

    public List<Venue> getAllVenuesSortedByDistance(double userLat, double userLon) {
        List<Venue> allVenues = getAllVenuesInCasablanca();
        for (Venue venue : allVenues) {
            double distance = calculateDistance(userLat, userLon, venue.getLatitude(), venue.getLongitude());
            venue.setDistanceKm(distance);
        }
        allVenues.sort((v1, v2) -> Double.compare(v1.getDistanceKm(), v2.getDistanceKm()));
        return allVenues;
    }

    public List<Venue> getVenuesByType(String type, double userLat, double userLon) {
        List<Venue> allVenues = getAllVenuesInCasablanca();
        List<Venue> filtered = new ArrayList<>();

        for (Venue venue : allVenues) {
            if (type.equalsIgnoreCase("All") || venue.getType().equalsIgnoreCase(type)) {
                double distance = calculateDistance(userLat, userLon, venue.getLatitude(), venue.getLongitude());
                venue.setDistanceKm(distance);
                filtered.add(venue);
            }
        }

        filtered.sort((v1, v2) -> Double.compare(v1.getDistanceKm(), v2.getDistanceKm()));
        return filtered;
    }

    public List<Venue> searchVenues(String query, double userLat, double userLon) {
        List<Venue> allVenues = getAllVenuesInCasablanca();
        List<Venue> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase(Locale.ROOT);

        for (Venue venue : allVenues) {
            boolean matches = venue.getName().toLowerCase(Locale.ROOT).contains(lowerQuery)
                    || venue.getType().toLowerCase(Locale.ROOT).contains(lowerQuery)
                    || (venue.getDescription() != null && venue.getDescription().toLowerCase(Locale.ROOT).contains(lowerQuery))
                    || (venue.getAddress() != null && venue.getAddress().toLowerCase(Locale.ROOT).contains(lowerQuery))
                    || (venue.getDistrict() != null && venue.getDistrict().toLowerCase(Locale.ROOT).contains(lowerQuery));

            if (!matches && venue.getTags() != null) {
                for (String tag : venue.getTags()) {
                    if (tag.toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                        matches = true;
                        break;
                    }
                }
            }

            if (!matches && venue.getVibeKeywords() != null) {
                for (String vibe : venue.getVibeKeywords()) {
                    if (vibe.toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                        matches = true;
                        break;
                    }
                }
            }

            if (matches) {
                double distance = calculateDistance(userLat, userLon, venue.getLatitude(), venue.getLongitude());
                venue.setDistanceKm(distance);
                filtered.add(venue);
            }
        }

        filtered.sort((v1, v2) -> Double.compare(v1.getDistanceKm(), v2.getDistanceKm()));
        return filtered;
    }

    public List<Venue> getVenuesByVibe(String vibe, double userLat, double userLon) {
        List<Venue> allVenues = getAllVenuesInCasablanca();
        List<Venue> filtered = new ArrayList<>();
        String lowerVibe = vibe.toLowerCase(Locale.ROOT);

        for (Venue venue : allVenues) {
            if (venue.getVibeKeywords() == null) continue;
            for (String v : venue.getVibeKeywords()) {
                if (v.toLowerCase(Locale.ROOT).contains(lowerVibe)) {
                    double distance = calculateDistance(userLat, userLon, venue.getLatitude(), venue.getLongitude());
                    venue.setDistanceKm(distance);
                    filtered.add(venue);
                    break;
                }
            }
        }
        filtered.sort((v1, v2) -> Double.compare(v1.getDistanceKm(), v2.getDistanceKm()));
        return filtered;
    }

    public Venue getVenueById(String id) {
        for (Venue v : getAllVenuesInCasablanca()) {
            if (v.getId() != null && v.getId().equals(id)) {
                return v;
            }
        }
        return null;
    }

    public List<Venue> getOpenNowVenues(double userLat, double userLon) {
        List<Venue> result = new ArrayList<>();
        for (Venue v : getAllVenuesInCasablanca()) {
            if (v.isOpenNow()) {
                v.setDistanceKm(calculateDistance(userLat, userLon, v.getLatitude(), v.getLongitude()));
                result.add(v);
            }
        }
        result.sort((v1, v2) -> Double.compare(v1.getDistanceKm(), v2.getDistanceKm()));
        return result;
    }

    public List<Venue> getTopRatedVenues(double userLat, double userLon, int limit) {
        List<Venue> allVenues = getAllVenuesInCasablanca();
        for (Venue v : allVenues) {
            v.setDistanceKm(calculateDistance(userLat, userLon, v.getLatitude(), v.getLongitude()));
        }
        allVenues.sort((v1, v2) -> Float.compare(v2.getRating(), v1.getRating()));
        return allVenues.subList(0, Math.min(limit, allVenues.size()));
    }

    private List<Venue> getAllVenuesInCasablanca() {
        if (cachedVenues != null) return new ArrayList<>(cachedVenues);
        List<Venue> v = new ArrayList<>();

        // CAFES
        v.add(build("Café de la Paix", "Cafe", 33.5928, -7.6192, "Centre Ville",
                "Avenue des Forces Armées Royales, Casablanca",
                "Historic French-style cafe in downtown Casablanca, perfect for morning coffee with pastries.",
                4.5f, 320, "☕", 2, 7, 23,
                new String[]{"coffee", "historic", "breakfast", "downtown"},
                new String[]{"classic", "chill", "morning"}));

        v.add(build("Bacha Coffee", "Cafe", 33.5870, -7.6310, "Anfa",
                "Anfa Place Mall, Casablanca",
                "Luxury coffee experience with 200+ rare beans from around the world.",
                4.7f, 215, "☕", 3, 9, 22,
                new String[]{"luxury", "specialty-coffee", "anfa"},
                new String[]{"upscale", "elegant", "premium"}));

        v.add(build("Paul Boulangerie", "Cafe", 33.5919, -7.6203, "Centre Ville",
                "Twin Center, Avenue Hassan II, Casablanca",
                "French bakery and cafe with fresh croissants and quiches.",
                4.4f, 678, "🥐", 2, 7, 21,
                new String[]{"bakery", "french", "pastries", "breakfast"},
                new String[]{"casual", "bright", "family"}));

        v.add(build("Starbucks Morocco Mall", "Cafe", 33.5279, -7.6610, "Ain Diab",
                "Morocco Mall, Boulevard de l'Ocean Atlantique",
                "International coffee chain inside the largest mall in Africa.",
                4.2f, 489, "☕", 2, 9, 23,
                new String[]{"coffee", "wifi", "shopping", "international"},
                new String[]{"casual", "lively", "wifi-friendly"}));

        v.add(build("Café Maure", "Cafe", 33.5985, -7.6320, "Centre Ville",
                "Place Mohammed V, Casablanca",
                "Traditional Moroccan cafe with mint tea, dates and pastries.",
                4.3f, 156, "🍵", 1, 8, 22,
                new String[]{"mint-tea", "traditional", "moroccan"},
                new String[]{"authentic", "chill", "local"}));

        v.add(build("Battoir Café", "Cafe", 33.5950, -7.6240, "Maarif",
                "Boulevard Mohammed Zerktouni, Casablanca",
                "Hipster coffee shop with specialty drinks and brunch.",
                4.5f, 412, "☕", 2, 8, 22,
                new String[]{"brunch", "specialty", "hipster", "wifi"},
                new String[]{"trendy", "modern", "instagrammable"}));

        // RESTAURANTS
        v.add(build("Le Cabestan", "Restaurant", 33.6056, -7.6818, "Ain Diab",
                "90 Boulevard de la Corniche, Phare El Hank, Casablanca",
                "Upscale seafood restaurant with stunning ocean views at the lighthouse.",
                4.7f, 1240, "🦞", 3, 12, 24,
                new String[]{"seafood", "fine-dining", "ocean-view", "romantic"},
                new String[]{"romantic", "elegant", "fancy"}));

        v.add(build("Rick's Café", "Restaurant", 33.6086, -7.6181, "Old Medina",
                "248 Boulevard Sour Jdid, Place du Jardin Public, Casablanca",
                "Iconic restaurant inspired by the movie Casablanca, with live jazz and piano.",
                4.5f, 2150, "🎷", 3, 18, 1,
                new String[]{"jazz", "iconic", "americana", "moroccan"},
                new String[]{"romantic", "nostalgic", "elegant"}));

        v.add(build("La Sqala", "Restaurant", 33.6010, -7.6210, "Old Medina",
                "Boulevard des Almohades, Casablanca",
                "Traditional Moroccan cuisine in a beautiful Andalusian-style garden inside old fortifications.",
                4.6f, 1856, "🥘", 2, 11, 23,
                new String[]{"moroccan", "traditional", "garden", "tagine"},
                new String[]{"authentic", "cozy", "romantic"}));

        v.add(build("Iloli Sushi", "Restaurant", 33.5876, -7.6376, "Anfa",
                "Boulevard d'Anfa, Casablanca",
                "Modern Japanese restaurant with creative sushi rolls and sashimi.",
                4.4f, 670, "🍣", 3, 12, 23,
                new String[]{"japanese", "sushi", "modern", "anfa"},
                new String[]{"trendy", "modern", "fresh"}));

        v.add(build("Le Relais de Paris", "Restaurant", 33.5887, -7.6342, "Anfa",
                "Tour Crystal 1, Casablanca Marina",
                "French steakhouse with famous secret sauce, premium beef.",
                4.5f, 920, "🥩", 3, 12, 23,
                new String[]{"french", "steak", "marina", "fine-dining"},
                new String[]{"romantic", "elegant", "premium"}));

        v.add(build("Dar Beida", "Restaurant", 33.6020, -7.6195, "Centre Ville",
                "10 Rue Idriss Lahrizi, Casablanca",
                "Authentic Moroccan home cooking - couscous, tagines, pastilla.",
                4.6f, 432, "🍲", 2, 12, 22,
                new String[]{"moroccan", "authentic", "traditional", "couscous"},
                new String[]{"authentic", "family", "warm"}));

        v.add(build("Basmane", "Restaurant", 33.5810, -7.6332, "Anfa",
                "Boulevard d'Anfa, Casablanca",
                "Modern Mediterranean cuisine, popular for business lunches.",
                4.4f, 543, "🍝", 3, 12, 23,
                new String[]{"mediterranean", "business-lunch", "modern"},
                new String[]{"trendy", "upscale", "lively"}));

        v.add(build("Tagine Darna", "Restaurant", 33.5895, -7.6210, "Maarif",
                "Rue Mohammed Smiha, Casablanca",
                "Casual spot famous for slow-cooked tagines and harira soup.",
                4.5f, 289, "🍲", 1, 11, 22,
                new String[]{"tagine", "moroccan", "casual", "lunch"},
                new String[]{"authentic", "cozy", "local"}));

        // BARS & NIGHTLIFE
        v.add(build("Sky 28", "Bar", 33.5876, -7.6309, "Maarif",
                "Kenzi Tower Hotel, Boulevard Mohammed Zerktouni",
                "Rooftop bar on the 28th floor with panoramic city views and signature cocktails.",
                4.4f, 540, "🍸", 3, 19, 2,
                new String[]{"rooftop", "cocktails", "view", "nightlife"},
                new String[]{"upscale", "romantic", "lively"}));

        v.add(build("Kinobar Casablanca", "Bar", 33.5872, -7.6298, "Maarif",
                "Rue El Hanania, Casablanca",
                "Trendy bar with live music and young crowd, popular among locals.",
                4.3f, 387, "🎸", 2, 19, 2,
                new String[]{"live-music", "trendy", "cocktails", "young-crowd"},
                new String[]{"lively", "trendy", "fun"}));

        v.add(build("La Bodega", "Bar", 33.5970, -7.6200, "Centre Ville",
                "129 Rue Allal Ben Abdellah, Casablanca",
                "Spanish tapas bar with great wine selection and Latin music nights.",
                4.5f, 612, "🍷", 2, 19, 1,
                new String[]{"tapas", "spanish", "wine", "latin"},
                new String[]{"lively", "fun", "dance"}));

        v.add(build("Le Trica", "Bar", 33.5882, -7.6312, "Maarif",
                "Centre 2000, Casablanca",
                "Cozy bar with eclectic atmosphere and creative cocktails.",
                4.2f, 290, "🍹", 2, 18, 1,
                new String[]{"cocktails", "cozy", "eclectic"},
                new String[]{"chill", "intimate", "creative"}));

        v.add(build("Bla Bla", "Bar", 33.5945, -7.6240, "Maarif",
                "Rue Pierre Parent, Casablanca",
                "Underground club with international DJs and electronic music.",
                4.3f, 478, "🎧", 3, 23, 5,
                new String[]{"club", "electronic", "dj", "nightlife"},
                new String[]{"energetic", "loud", "party"}));

        // ACTIVITIES
        v.add(build("Hassan II Mosque", "Activity", 33.6084, -7.6325, "Old Medina",
                "Boulevard de la Corniche, Casablanca",
                "World's third largest mosque with stunning architecture and ocean views. Open to non-Muslims via guided tour.",
                4.8f, 18500, "🕌", 1, 9, 18,
                new String[]{"cultural", "must-see", "architecture", "religious"},
                new String[]{"iconic", "spiritual", "majestic"}));

        v.add(build("Corniche Ain Diab", "Activity", 33.5950, -7.6800, "Ain Diab",
                "Boulevard de la Corniche, Casablanca",
                "Famous beach promenade with cafes, restaurants, and beach clubs along the Atlantic.",
                4.5f, 5670, "🌊", 1, 0, 24,
                new String[]{"beach", "promenade", "outdoor", "sunset"},
                new String[]{"relaxing", "scenic", "fun"}));

        v.add(build("Old Medina", "Activity", 33.6024, -7.6196, "Old Medina",
                "Old Medina, Casablanca",
                "Historic walled medina with traditional souks, leather goods, spices and crafts.",
                4.2f, 3210, "🏛️", 1, 8, 22,
                new String[]{"historic", "shopping", "souk", "cultural"},
                new String[]{"authentic", "bustling", "cultural"}));

        v.add(build("Villa des Arts", "Activity", 33.5908, -7.6285, "Maarif",
                "30 Boulevard Brahim Roudani, Casablanca",
                "Contemporary art museum showcasing Moroccan and international artists.",
                4.4f, 287, "🎨", 1, 10, 19,
                new String[]{"art", "museum", "cultural", "modern"},
                new String[]{"creative", "inspiring", "quiet"}));

        v.add(build("Cathédrale Sacré-Cœur", "Activity", 33.5872, -7.6232, "Centre Ville",
                "Boulevard Rachidi, Casablanca",
                "Former Catholic church now an art exhibition space with unique architecture.",
                4.4f, 1150, "⛪", 1, 9, 18,
                new String[]{"architecture", "historic", "art"},
                new String[]{"unique", "quiet", "beautiful"}));

        v.add(build("Casablanca Marina", "Activity", 33.6045, -7.6189, "Old Medina",
                "Boulevard des Almohades, Casablanca",
                "Modern waterfront development with luxury boats, restaurants and walking areas.",
                4.5f, 1850, "⛵", 2, 8, 24,
                new String[]{"waterfront", "modern", "restaurants", "luxury"},
                new String[]{"modern", "scenic", "upscale"}));

        v.add(build("Mahkama du Pacha", "Activity", 33.5728, -7.5985, "Habous",
                "Quartier Habous, Casablanca",
                "Stunning 1950s building with carved cedar ceilings and Moorish art.",
                4.6f, 421, "🏛️", 1, 9, 17,
                new String[]{"architecture", "historic", "habous", "hidden-gem"},
                new String[]{"unique", "quiet", "majestic"}));

        v.add(build("Tamaris Aquaparc", "Activity", 33.5108, -7.7253, "Dar Bouazza",
                "Route Cotière Dar Bouazza, Casablanca",
                "Large waterpark with pools, slides and family attractions.",
                4.3f, 1820, "🌊", 2, 10, 19,
                new String[]{"waterpark", "family", "fun", "summer"},
                new String[]{"fun", "energetic", "family"}));

        // PARKS
        v.add(build("Parc de la Ligue Arabe", "Park", 33.5910, -7.6232, "Centre Ville",
                "Boulevard Moulay Youssef, Casablanca",
                "Largest urban park in Casablanca, perfect for jogging and picnics.",
                4.3f, 1890, "🌳", 1, 6, 22,
                new String[]{"park", "outdoor", "jogging", "picnic"},
                new String[]{"relaxing", "green", "family"}));

        v.add(build("Anfa Park", "Park", 33.5750, -7.6431, "Anfa",
                "Anfa, Casablanca",
                "Modern park in upscale Anfa district with playgrounds and walking paths.",
                4.4f, 920, "🌳", 1, 6, 23,
                new String[]{"park", "modern", "family-friendly"},
                new String[]{"relaxing", "modern", "family"}));

        v.add(build("Sindibad Beach Resort", "Park", 33.5915, -7.6862, "Ain Diab",
                "Boulevard de la Corniche, Casablanca",
                "Amusement park with rides, zoo, and ocean views.",
                4.0f, 2340, "🎢", 2, 10, 22,
                new String[]{"amusement-park", "rides", "family", "zoo"},
                new String[]{"fun", "energetic", "family"}));

        // SHOPPING
        v.add(build("Morocco Mall", "Shopping", 33.5279, -7.6610, "Ain Diab",
                "Boulevard de l'Ocean Atlantique, Casablanca",
                "Largest shopping mall in Africa with international brands, aquarium and IMAX cinema.",
                4.5f, 8740, "🛍️", 2, 10, 23,
                new String[]{"shopping", "mall", "entertainment", "aquarium"},
                new String[]{"lively", "modern", "family"}));

        v.add(build("Anfa Place", "Shopping", 33.5870, -7.6310, "Ain Diab",
                "Boulevard de la Corniche, Casablanca",
                "Upscale beachfront shopping center with cafes and ocean views.",
                4.4f, 2310, "🏖️", 2, 10, 22,
                new String[]{"shopping", "beachfront", "upscale"},
                new String[]{"upscale", "modern", "scenic"}));

        v.add(build("Marché Central", "Shopping", 33.5970, -7.6184, "Centre Ville",
                "Rue Allal Ben Abdellah, Casablanca",
                "Bustling local market with fresh seafood, produce, and flowers.",
                4.4f, 1280, "🐟", 1, 6, 18,
                new String[]{"market", "local", "fresh-food", "authentic"},
                new String[]{"authentic", "bustling", "local"}));

        v.add(build("Twin Center", "Shopping", 33.5919, -7.6203, "Centre Ville",
                "Boulevard Zerktouni, Casablanca",
                "Iconic twin towers with shops, cafes and offices in the city center.",
                4.2f, 1120, "🏬", 2, 9, 22,
                new String[]{"shopping", "downtown", "iconic"},
                new String[]{"modern", "central", "busy"}));

        // Apply per-venue photo + review overlay
        for (Venue venue : v) {
            enrichVenue(venue);
        }

        cachedVenues = v;
        return new ArrayList<>(v);
    }

    private Venue build(String name, String type, double lat, double lon, String district,
                        String address, String description, float rating, int reviews, String emoji,
                        int price, int openH, int closeH, String[] tags, String[] vibes) {
        Venue venue = new Venue(name, type, lat, lon, address);
        venue.setDistrict(district);
        venue.setDescription(description);
        venue.setRating(rating);
        venue.setReviewCount(reviews);
        venue.setImageEmoji(emoji);
        venue.setPriceLevel(price);
        venue.setOpenHour(openH);
        venue.setCloseHour(closeH);
        venue.setTags(tags);
        venue.setVibeKeywords(vibes);
        // Default photo by venue type (real Unsplash URL) — will be overridden by enrichVenue()
        venue.setImageUrl(getDefaultImageUrl(type));
        return venue;
    }

    /**
     * Overlay real photos + curated reviews for each specific venue.
     * Called after the base build to add venue-specific details.
     */
    private void enrichVenue(Venue v) {
        String id = v.getId();
        if (id == null) return;

        switch (id) {
            // ============ CAFES ============
            case "cafe_de_la_paix":
                v.setImageUrl("https://images.unsplash.com/photo-1559925393-8be0ec4767c8?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Step back in time! The Art Deco interior is gorgeous and the cappuccino is excellent. A Casa classic.");
                v.setTopReviewAuthor("Sarah M.");
                break;
            case "bacha_coffee":
                v.setImageUrl("https://images.unsplash.com/photo-1442975631115-c4f7b05b8a2c?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("The atmosphere is incredible — like a coffee museum. Pricey but every cup is an experience.");
                v.setTopReviewAuthor("Karim B.");
                break;
            case "paul_boulangerie":
                v.setImageUrl("https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Best croissants in Casablanca. The pain au chocolat is to die for — my morning ritual.");
                v.setTopReviewAuthor("Yasmine T.");
                break;
            case "starbucks_morocco_mall":
                v.setImageUrl("https://images.unsplash.com/photo-1453614512568-c4024d13c247?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Convenient location with strong wifi. Perfect for working between shopping. Pumpkin spice in season is great.");
                v.setTopReviewAuthor("Reda E.");
                break;
            case "cafe_maure":
                v.setImageUrl("https://images.unsplash.com/photo-1597318181409-cf64d0b5d8a2?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Authentic mint tea ceremony. The waiters in traditional outfits make it feel special. Very affordable.");
                v.setTopReviewAuthor("Amine L.");
                break;
            case "battoir_cafe":
                v.setImageUrl("https://images.unsplash.com/photo-1525610553991-2bede1a236e2?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Best brunch spot in Maarif. Avocado toast and matcha latte are insta-worthy. Trendy vibe!");
                v.setTopReviewAuthor("Leila K.");
                break;

            // ============ RESTAURANTS ============
            case "le_cabestan":
                v.setImageUrl("https://images.unsplash.com/photo-1559339352-11d035aa65de?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Best seafood in Casa, no contest. Watching the waves crash while eating fresh lobster — unforgettable.");
                v.setTopReviewAuthor("Mehdi R.");
                break;
            case "ricks_cafe":
                v.setImageUrl("https://images.unsplash.com/photo-1470337458703-46ad1756a187?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Yes it's touristy. Yes it's worth it. The live piano playing 'As Time Goes By' gave me chills.");
                v.setTopReviewAuthor("Sophia D.");
                break;
            case "la_sqala":
                v.setImageUrl("https://images.unsplash.com/photo-1493770348161-369560ae357d?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("The Andalusian garden is magical. Their lamb tagine with prunes is the best I've had in Morocco.");
                v.setTopReviewAuthor("Hassan A.");
                break;
            case "iloli_sushi":
                v.setImageUrl("https://images.unsplash.com/photo-1579871494447-9811cf80d66c?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Surprisingly good sushi for Casablanca! Fresh fish, creative rolls. The Iloli Special is amazing.");
                v.setTopReviewAuthor("Nadia M.");
                break;
            case "le_relais_de_paris":
                v.setImageUrl("https://images.unsplash.com/photo-1544025162-d76694265947?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("The secret sauce really is magic. Perfect steak-frites every time. Romantic spot with marina views.");
                v.setTopReviewAuthor("Pierre L.");
                break;
            case "dar_beida":
                v.setImageUrl("https://images.unsplash.com/photo-1530469912745-a215c6b256ea?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Eating here feels like grandma's cooking. The pastilla and couscous are exceptional. Hidden gem.");
                v.setTopReviewAuthor("Fatima B.");
                break;
            case "basmane":
                v.setImageUrl("https://images.unsplash.com/photo-1592861956120-e524fc739696?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Stylish spot for business lunch. Modern Mediterranean done right. The truffle pasta is divine.");
                v.setTopReviewAuthor("Omar S.");
                break;
            case "tagine_darna":
                v.setImageUrl("https://images.unsplash.com/photo-1581873372796-635b67ca2008?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Best value tagine in town. Harira soup is what my grandmother makes. Local favorite for a reason.");
                v.setTopReviewAuthor("Aicha N.");
                break;

            // ============ BARS ============
            case "sky_28":
                v.setImageUrl("https://images.unsplash.com/photo-1566417713940-fe7c737a9ef2?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("The view from the 28th floor is mind-blowing, especially at sunset. Cocktails are creative. Dress up!");
                v.setTopReviewAuthor("Tarik H.");
                break;
            case "kinobar":
                v.setImageUrl("https://images.unsplash.com/photo-1514933651103-005eec06c04b?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Live music every weekend, great young crowd. Best place to actually have a conversation while having fun.");
                v.setTopReviewAuthor("Youssef A.");
                break;
            case "la_bodega":
                v.setImageUrl("https://images.unsplash.com/photo-1510812431401-41d2bd2722f3?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Tapas, sangria, and salsa nights. Saturday nights here are legendary. Authentic Spanish vibe.");
                v.setTopReviewAuthor("Maria F.");
                break;
            case "le_trica":
                v.setImageUrl("https://images.unsplash.com/photo-1572116469696-31de0f17cc34?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Hidden cocktail bar, very chill vibe. Their mixologists are skilled. Perfect for date night.");
                v.setTopReviewAuthor("Said B.");
                break;
            case "bla_bla":
                v.setImageUrl("https://images.unsplash.com/photo-1571266028243-d220c6a45c89?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Best techno nights in Casa. International DJs every weekend. Stay until dawn — it gets wild around 2am.");
                v.setTopReviewAuthor("DJ Karim");
                break;

            // ============ ACTIVITIES ============
            case "hassan_ii_mosque":
                v.setImageUrl("https://images.unsplash.com/photo-1577378040608-bf41815e60d3?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Absolutely breathtaking. Take the guided tour — the craftsmanship inside is incredible. Best at sunset.");
                v.setTopReviewAuthor("Anna K.");
                break;
            case "corniche_ain_diab":
                v.setImageUrl("https://images.unsplash.com/photo-1518509562904-e7ef99cddc85?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Walk the whole Corniche at sunset — it's beautiful. Stop at the beach clubs along the way for a drink.");
                v.setTopReviewAuthor("Ahmed J.");
                break;
            case "old_medina":
                v.setImageUrl("https://images.unsplash.com/photo-1565689157206-0fddef7589a2?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Bring cash and patience for haggling. Found beautiful leather goods and spices. Smaller than Marrakech medina but charming.");
                v.setTopReviewAuthor("Linda T.");
                break;
            case "villa_des_arts":
                v.setImageUrl("https://images.unsplash.com/photo-1544967082-d9d25d867d66?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Free entry and rotating contemporary exhibits. The garden alone is worth the visit. Quiet escape from the city.");
                v.setTopReviewAuthor("Camille O.");
                break;
            case "cathedrale_sacre_coeur":
                v.setImageUrl("https://images.unsplash.com/photo-1548013146-72479768bada?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Stunning neo-gothic architecture. No longer a church but the space is gorgeous and often hosts art shows.");
                v.setTopReviewAuthor("Marc D.");
                break;
            case "casablanca_marina":
                v.setImageUrl("https://images.unsplash.com/photo-1591375372088-d10bb09891ef?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Beautiful modern marina, great for evening walks. Lots of restaurants. Sunset cocktails here are perfect.");
                v.setTopReviewAuthor("Khalid M.");
                break;
            case "mahkama_du_pacha":
                v.setImageUrl("https://images.unsplash.com/photo-1542401886-65d6c61db217?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Hidden treasure! The carved cedar ceilings and tile work are stunning. Most tourists miss this — don't.");
                v.setTopReviewAuthor("Rachid F.");
                break;
            case "tamaris_aquaparc":
                v.setImageUrl("https://images.unsplash.com/photo-1530549387789-4c1017266635?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Great fun on hot summer days. The kids loved the lazy river. Bring sunscreen — there's not much shade.");
                v.setTopReviewAuthor("Amal Z.");
                break;

            // ============ PARKS ============
            case "parc_de_la_ligue_arabe":
                v.setImageUrl("https://images.unsplash.com/photo-1519331379826-f10be5486c6f?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("My morning running spot! Big trees, peaceful. Get a fresh juice from one of the stalls afterward.");
                v.setTopReviewAuthor("Ibrahim S.");
                break;
            case "anfa_park":
                v.setImageUrl("https://images.unsplash.com/photo-1492571350019-22de08371fd3?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Modern and well-maintained. Great playground for kids. Always feels safe and clean.");
                v.setTopReviewAuthor("Salma R.");
                break;
            case "sindibad_beach_resort":
                v.setImageUrl("https://images.unsplash.com/photo-1583244532610-2a234c8b81e9?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Old-school amusement park with charm. Kids had a blast. Some rides need updating but views are nice.");
                v.setTopReviewAuthor("Hamza K.");
                break;

            // ============ SHOPPING ============
            case "morocco_mall":
                v.setImageUrl("https://images.unsplash.com/photo-1519567241046-7f570eee3ce6?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Massive mall, all the international brands. The musical fountain show is fun. Aquarium is worth a visit.");
                v.setTopReviewAuthor("Zineb O.");
                break;
            case "anfa_place":
                v.setImageUrl("https://images.unsplash.com/photo-1481437156560-3205f6a55735?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Smaller than Morocco Mall but more pleasant. Love the cafes facing the ocean. Less crowded too.");
                v.setTopReviewAuthor("Bilal H.");
                break;
            case "marche_central":
                v.setImageUrl("https://images.unsplash.com/photo-1488459716781-31db52582fe9?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Best seafood prices in town. Pick your fish and grill it on the spot at one of the stalls. So fresh!");
                v.setTopReviewAuthor("Latifa Q.");
                break;
            case "twin_center":
                v.setImageUrl("https://images.unsplash.com/photo-1483653364400-eedcfb9f1f88?w=900&auto=format&fit=crop&q=80");
                v.setTopReview("Iconic towers visible from everywhere. Decent shopping mix and Paul on the ground floor is great.");
                v.setTopReviewAuthor("Sami T.");
                break;
        }
    }

    private String getDefaultImageUrl(String type) {
        if (type == null) return "https://images.unsplash.com/photo-1571266028243-d220c6a45c89?w=800&auto=format&fit=crop&q=80";
        switch (type.toLowerCase()) {
            case "cafe": return "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=800&auto=format&fit=crop&q=80";
            case "restaurant": return "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=800&auto=format&fit=crop&q=80";
            case "bar": return "https://images.unsplash.com/photo-1566417713940-fe7c737a9ef2?w=800&auto=format&fit=crop&q=80";
            case "activity": return "https://images.unsplash.com/photo-1597212849308-c87f9bb73fa2?w=800&auto=format&fit=crop&q=80";
            case "park": return "https://images.unsplash.com/photo-1572636356942-7c66c4b6e8c0?w=800&auto=format&fit=crop&q=80";
            case "shopping": return "https://images.unsplash.com/photo-1519567241046-7f570eee3ce6?w=800&auto=format&fit=crop&q=80";
            default: return "https://images.unsplash.com/photo-1571266028243-d220c6a45c89?w=800&auto=format&fit=crop&q=80";
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public static String[] getCategories() {
        return new String[]{"All", "Cafe", "Restaurant", "Bar", "Activity", "Park", "Shopping"};
    }

    public static String[] getVibes() {
        return new String[]{"romantic", "chill", "lively", "authentic", "modern", "trendy", "family", "elegant"};
    }

    public static String[] getDistricts() {
        return new String[]{"All", "Centre Ville", "Anfa", "Maarif", "Ain Diab", "Old Medina", "Habous", "Dar Bouazza"};
    }
}
