-- ====================================================================
-- VibeMeet — Seed data: 33 real Casablanca venues
-- ====================================================================

INSERT INTO public.venues (id, name, type, district, latitude, longitude, address, description, rating, review_count, image_emoji, price_level, open_hour, close_hour, tags, vibe_keywords) VALUES
-- Cafes
('cafe_de_la_paix', 'Café de la Paix', 'Cafe', 'Centre Ville', 33.5928, -7.6192, 'Avenue des Forces Armées Royales, Casablanca', 'Historic French-style cafe in downtown Casablanca, perfect for morning coffee with pastries.', 4.5, 320, '☕', 2, 7, 23, ARRAY['coffee','historic','breakfast','downtown'], ARRAY['classic','chill','morning']),
('bacha_coffee', 'Bacha Coffee', 'Cafe', 'Anfa', 33.5870, -7.6310, 'Anfa Place Mall, Casablanca', 'Luxury coffee experience with 200+ rare beans from around the world.', 4.7, 215, '☕', 3, 9, 22, ARRAY['luxury','specialty-coffee','anfa'], ARRAY['upscale','elegant','premium']),
('paul_boulangerie', 'Paul Boulangerie', 'Cafe', 'Centre Ville', 33.5919, -7.6203, 'Twin Center, Avenue Hassan II, Casablanca', 'French bakery and cafe with fresh croissants and quiches.', 4.4, 678, '🥐', 2, 7, 21, ARRAY['bakery','french','pastries','breakfast'], ARRAY['casual','bright','family']),
('starbucks_morocco_mall', 'Starbucks Morocco Mall', 'Cafe', 'Ain Diab', 33.5279, -7.6610, 'Morocco Mall, Boulevard de l''Ocean Atlantique', 'International coffee chain inside the largest mall in Africa.', 4.2, 489, '☕', 2, 9, 23, ARRAY['coffee','wifi','shopping','international'], ARRAY['casual','lively','wifi-friendly']),
('cafe_maure', 'Café Maure', 'Cafe', 'Centre Ville', 33.5985, -7.6320, 'Place Mohammed V, Casablanca', 'Traditional Moroccan cafe with mint tea, dates and pastries.', 4.3, 156, '🍵', 1, 8, 22, ARRAY['mint-tea','traditional','moroccan'], ARRAY['authentic','chill','local']),
('battoir_cafe', 'Battoir Café', 'Cafe', 'Maarif', 33.5950, -7.6240, 'Boulevard Mohammed Zerktouni, Casablanca', 'Hipster coffee shop with specialty drinks and brunch.', 4.5, 412, '☕', 2, 8, 22, ARRAY['brunch','specialty','hipster','wifi'], ARRAY['trendy','modern','instagrammable']),

-- Restaurants
('le_cabestan', 'Le Cabestan', 'Restaurant', 'Ain Diab', 33.6056, -7.6818, '90 Boulevard de la Corniche, Phare El Hank, Casablanca', 'Upscale seafood restaurant with stunning ocean views at the lighthouse.', 4.7, 1240, '🦞', 3, 12, 24, ARRAY['seafood','fine-dining','ocean-view','romantic'], ARRAY['romantic','elegant','fancy']),
('ricks_cafe', 'Rick''s Café', 'Restaurant', 'Old Medina', 33.6086, -7.6181, '248 Boulevard Sour Jdid, Place du Jardin Public, Casablanca', 'Iconic restaurant inspired by the movie Casablanca, with live jazz and piano.', 4.5, 2150, '🎷', 3, 18, 1, ARRAY['jazz','iconic','americana','moroccan'], ARRAY['romantic','nostalgic','elegant']),
('la_sqala', 'La Sqala', 'Restaurant', 'Old Medina', 33.6010, -7.6210, 'Boulevard des Almohades, Casablanca', 'Traditional Moroccan cuisine in a beautiful Andalusian-style garden inside old fortifications.', 4.6, 1856, '🥘', 2, 11, 23, ARRAY['moroccan','traditional','garden','tagine'], ARRAY['authentic','cozy','romantic']),
('iloli_sushi', 'Iloli Sushi', 'Restaurant', 'Anfa', 33.5876, -7.6376, 'Boulevard d''Anfa, Casablanca', 'Modern Japanese restaurant with creative sushi rolls and sashimi.', 4.4, 670, '🍣', 3, 12, 23, ARRAY['japanese','sushi','modern','anfa'], ARRAY['trendy','modern','fresh']),
('le_relais_de_paris', 'Le Relais de Paris', 'Restaurant', 'Anfa', 33.5887, -7.6342, 'Tour Crystal 1, Casablanca Marina', 'French steakhouse with famous secret sauce, premium beef.', 4.5, 920, '🥩', 3, 12, 23, ARRAY['french','steak','marina','fine-dining'], ARRAY['romantic','elegant','premium']),
('dar_beida', 'Dar Beida', 'Restaurant', 'Centre Ville', 33.6020, -7.6195, '10 Rue Idriss Lahrizi, Casablanca', 'Authentic Moroccan home cooking - couscous, tagines, pastilla.', 4.6, 432, '🍲', 2, 12, 22, ARRAY['moroccan','authentic','traditional','couscous'], ARRAY['authentic','family','warm']),
('basmane', 'Basmane', 'Restaurant', 'Anfa', 33.5810, -7.6332, 'Boulevard d''Anfa, Casablanca', 'Modern Mediterranean cuisine, popular for business lunches.', 4.4, 543, '🍝', 3, 12, 23, ARRAY['mediterranean','business-lunch','modern'], ARRAY['trendy','upscale','lively']),
('tagine_darna', 'Tagine Darna', 'Restaurant', 'Maarif', 33.5895, -7.6210, 'Rue Mohammed Smiha, Casablanca', 'Casual spot famous for slow-cooked tagines and harira soup.', 4.5, 289, '🍲', 1, 11, 22, ARRAY['tagine','moroccan','casual','lunch'], ARRAY['authentic','cozy','local']),

-- Bars
('sky_28', 'Sky 28', 'Bar', 'Maarif', 33.5876, -7.6309, 'Kenzi Tower Hotel, Boulevard Mohammed Zerktouni', 'Rooftop bar on the 28th floor with panoramic city views and signature cocktails.', 4.4, 540, '🍸', 3, 19, 2, ARRAY['rooftop','cocktails','view','nightlife'], ARRAY['upscale','romantic','lively']),
('kinobar', 'Kinobar Casablanca', 'Bar', 'Maarif', 33.5872, -7.6298, 'Rue El Hanania, Casablanca', 'Trendy bar with live music and young crowd, popular among locals.', 4.3, 387, '🎸', 2, 19, 2, ARRAY['live-music','trendy','cocktails','young-crowd'], ARRAY['lively','trendy','fun']),
('la_bodega', 'La Bodega', 'Bar', 'Centre Ville', 33.5970, -7.6200, '129 Rue Allal Ben Abdellah, Casablanca', 'Spanish tapas bar with great wine selection and Latin music nights.', 4.5, 612, '🍷', 2, 19, 1, ARRAY['tapas','spanish','wine','latin'], ARRAY['lively','fun','dance']),
('le_trica', 'Le Trica', 'Bar', 'Maarif', 33.5882, -7.6312, 'Centre 2000, Casablanca', 'Cozy bar with eclectic atmosphere and creative cocktails.', 4.2, 290, '🍹', 2, 18, 1, ARRAY['cocktails','cozy','eclectic'], ARRAY['chill','intimate','creative']),
('bla_bla', 'Bla Bla', 'Bar', 'Maarif', 33.5945, -7.6240, 'Rue Pierre Parent, Casablanca', 'Underground club with international DJs and electronic music.', 4.3, 478, '🎧', 3, 23, 5, ARRAY['club','electronic','dj','nightlife'], ARRAY['energetic','loud','party']),

-- Activities
('hassan_ii_mosque', 'Hassan II Mosque', 'Activity', 'Old Medina', 33.6084, -7.6325, 'Boulevard de la Corniche, Casablanca', 'World''s third largest mosque with stunning architecture and ocean views. Open to non-Muslims via guided tour.', 4.8, 18500, '🕌', 1, 9, 18, ARRAY['cultural','must-see','architecture','religious'], ARRAY['iconic','spiritual','majestic']),
('corniche_ain_diab', 'Corniche Ain Diab', 'Activity', 'Ain Diab', 33.5950, -7.6800, 'Boulevard de la Corniche, Casablanca', 'Famous beach promenade with cafes, restaurants, and beach clubs along the Atlantic.', 4.5, 5670, '🌊', 1, 0, 24, ARRAY['beach','promenade','outdoor','sunset'], ARRAY['relaxing','scenic','fun']),
('old_medina', 'Old Medina', 'Activity', 'Old Medina', 33.6024, -7.6196, 'Old Medina, Casablanca', 'Historic walled medina with traditional souks, leather goods, spices and crafts.', 4.2, 3210, '🏛️', 1, 8, 22, ARRAY['historic','shopping','souk','cultural'], ARRAY['authentic','bustling','cultural']),
('villa_des_arts', 'Villa des Arts', 'Activity', 'Maarif', 33.5908, -7.6285, '30 Boulevard Brahim Roudani, Casablanca', 'Contemporary art museum showcasing Moroccan and international artists.', 4.4, 287, '🎨', 1, 10, 19, ARRAY['art','museum','cultural','modern'], ARRAY['creative','inspiring','quiet']),
('cathedrale_sacre_coeur', 'Cathédrale Sacré-Cœur', 'Activity', 'Centre Ville', 33.5872, -7.6232, 'Boulevard Rachidi, Casablanca', 'Former Catholic church now an art exhibition space with unique architecture.', 4.4, 1150, '⛪', 1, 9, 18, ARRAY['architecture','historic','art'], ARRAY['unique','quiet','beautiful']),
('casablanca_marina', 'Casablanca Marina', 'Activity', 'Old Medina', 33.6045, -7.6189, 'Boulevard des Almohades, Casablanca', 'Modern waterfront development with luxury boats, restaurants and walking areas.', 4.5, 1850, '⛵', 2, 8, 24, ARRAY['waterfront','modern','restaurants','luxury'], ARRAY['modern','scenic','upscale']),
('mahkama_du_pacha', 'Mahkama du Pacha', 'Activity', 'Habous', 33.5728, -7.5985, 'Quartier Habous, Casablanca', 'Stunning 1950s building with carved cedar ceilings and Moorish art.', 4.6, 421, '🏛️', 1, 9, 17, ARRAY['architecture','historic','habous','hidden-gem'], ARRAY['unique','quiet','majestic']),
('tamaris_aquaparc', 'Tamaris Aquaparc', 'Activity', 'Dar Bouazza', 33.5108, -7.7253, 'Route Cotière Dar Bouazza, Casablanca', 'Large waterpark with pools, slides and family attractions.', 4.3, 1820, '🌊', 2, 10, 19, ARRAY['waterpark','family','fun','summer'], ARRAY['fun','energetic','family']),

-- Parks
('parc_de_la_ligue_arabe', 'Parc de la Ligue Arabe', 'Park', 'Centre Ville', 33.5910, -7.6232, 'Boulevard Moulay Youssef, Casablanca', 'Largest urban park in Casablanca, perfect for jogging and picnics.', 4.3, 1890, '🌳', 1, 6, 22, ARRAY['park','outdoor','jogging','picnic'], ARRAY['relaxing','green','family']),
('anfa_park', 'Anfa Park', 'Park', 'Anfa', 33.5750, -7.6431, 'Anfa, Casablanca', 'Modern park in upscale Anfa district with playgrounds and walking paths.', 4.4, 920, '🌳', 1, 6, 23, ARRAY['park','modern','family-friendly'], ARRAY['relaxing','modern','family']),
('sindibad_beach_resort', 'Sindibad Beach Resort', 'Park', 'Ain Diab', 33.5915, -7.6862, 'Boulevard de la Corniche, Casablanca', 'Amusement park with rides, zoo, and ocean views.', 4.0, 2340, '🎢', 2, 10, 22, ARRAY['amusement-park','rides','family','zoo'], ARRAY['fun','energetic','family']),

-- Shopping
('morocco_mall', 'Morocco Mall', 'Shopping', 'Ain Diab', 33.5279, -7.6610, 'Boulevard de l''Ocean Atlantique, Casablanca', 'Largest shopping mall in Africa with international brands, aquarium and IMAX cinema.', 4.5, 8740, '🛍️', 2, 10, 23, ARRAY['shopping','mall','entertainment','aquarium'], ARRAY['lively','modern','family']),
('anfa_place', 'Anfa Place', 'Shopping', 'Ain Diab', 33.5870, -7.6310, 'Boulevard de la Corniche, Casablanca', 'Upscale beachfront shopping center with cafes and ocean views.', 4.4, 2310, '🏖️', 2, 10, 22, ARRAY['shopping','beachfront','upscale'], ARRAY['upscale','modern','scenic']),
('marche_central', 'Marché Central', 'Shopping', 'Centre Ville', 33.5970, -7.6184, 'Rue Allal Ben Abdellah, Casablanca', 'Bustling local market with fresh seafood, produce, and flowers.', 4.4, 1280, '🐟', 1, 6, 18, ARRAY['market','local','fresh-food','authentic'], ARRAY['authentic','bustling','local']),
('twin_center', 'Twin Center', 'Shopping', 'Centre Ville', 33.5919, -7.6203, 'Boulevard Zerktouni, Casablanca', 'Iconic twin towers with shops, cafes and offices in the city center.', 4.2, 1120, '🏬', 2, 9, 22, ARRAY['shopping','downtown','iconic'], ARRAY['modern','central','busy'])

ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    type = EXCLUDED.type,
    description = EXCLUDED.description,
    rating = EXCLUDED.rating,
    review_count = EXCLUDED.review_count,
    image_emoji = EXCLUDED.image_emoji,
    tags = EXCLUDED.tags,
    vibe_keywords = EXCLUDED.vibe_keywords;
