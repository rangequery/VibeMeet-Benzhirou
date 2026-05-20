-- ====================================================================
-- Add image_url + sample reviews snippet to venues
-- ====================================================================

ALTER TABLE public.venues
    ADD COLUMN IF NOT EXISTS image_url TEXT,
    ADD COLUMN IF NOT EXISTS top_review TEXT,
    ADD COLUMN IF NOT EXISTS top_review_author TEXT;

-- Add sample real-looking reviews for venues to be displayed in the app
-- (These would normally come from Google Places API or similar)

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=800&auto=format&fit=crop&q=80',
    top_review = 'Authentic Parisian atmosphere right in the heart of Casablanca. The croissants are amazing!',
    top_review_author = 'Sarah M.'
WHERE id = 'cafe_de_la_paix';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1497935586351-b67a49e012bf?w=800&auto=format&fit=crop&q=80',
    top_review = 'The coffee selection is incredible. A real luxury experience worth the price.',
    top_review_author = 'Karim B.'
WHERE id = 'bacha_coffee';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=800&auto=format&fit=crop&q=80',
    top_review = 'Fresh croissants every morning. My go-to breakfast spot before work.',
    top_review_author = 'Yasmine T.'
WHERE id = 'paul_boulangerie';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1453614512568-c4024d13c247?w=800&auto=format&fit=crop&q=80',
    top_review = 'Convenient location inside Morocco Mall. Good wifi, perfect for working.',
    top_review_author = 'Reda E.'
WHERE id = 'starbucks_morocco_mall';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1571934811356-5cc061b6821f?w=800&auto=format&fit=crop&q=80',
    top_review = 'Traditional mint tea served beautifully. Charming traditional atmosphere.',
    top_review_author = 'Amine L.'
WHERE id = 'cafe_maure';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1442975631115-c4f7b05b8a2c?w=800&auto=format&fit=crop&q=80',
    top_review = 'Best brunch in the city! The avocado toast is to die for.',
    top_review_author = 'Leila K.'
WHERE id = 'battoir_cafe';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1559339352-11d035aa65de?w=800&auto=format&fit=crop&q=80',
    top_review = 'Magical sunset views over the Atlantic. The lobster was perfectly cooked.',
    top_review_author = 'Mehdi R.'
WHERE id = 'le_cabestan';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=800&auto=format&fit=crop&q=80',
    top_review = 'A piece of cinema history! Live piano makes it unforgettable. Iconic!',
    top_review_author = 'Sophia D.'
WHERE id = 'ricks_cafe';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1535860434060-3a8d2af3f8a6?w=800&auto=format&fit=crop&q=80',
    top_review = 'Authentic Moroccan cuisine in a stunning Andalusian garden setting.',
    top_review_author = 'Hassan A.'
WHERE id = 'la_sqala';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1579871494447-9811cf80d66c?w=800&auto=format&fit=crop&q=80',
    top_review = 'Best sushi in Casablanca. Fresh ingredients and creative rolls.',
    top_review_author = 'Nadia M.'
WHERE id = 'iloli_sushi';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1544025162-d76694265947?w=800&auto=format&fit=crop&q=80',
    top_review = 'The secret sauce really is amazing. Excellent steak, French elegance.',
    top_review_author = 'Pierre L.'
WHERE id = 'le_relais_de_paris';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1539136788836-5699e78bfc75?w=800&auto=format&fit=crop&q=80',
    top_review = 'Tastes just like my grandmother''s tagine. Authentic and affordable!',
    top_review_author = 'Fatima B.'
WHERE id = 'dar_beida';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1565958011703-44f9829ba187?w=800&auto=format&fit=crop&q=80',
    top_review = 'Modern, fresh, perfect for a business lunch. Pasta dishes are exquisite.',
    top_review_author = 'Omar S.'
WHERE id = 'basmane';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1542528180-a1208c5169a5?w=800&auto=format&fit=crop&q=80',
    top_review = 'Best tagine in Casablanca for the price! Cozy local spot.',
    top_review_author = 'Aicha N.'
WHERE id = 'tagine_darna';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1566417713940-fe7c737a9ef2?w=800&auto=format&fit=crop&q=80',
    top_review = 'Stunning panoramic views! The cocktails are creative and the music vibe is perfect.',
    top_review_author = 'Tarik H.'
WHERE id = 'sky_28';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1514933651103-005eec06c04b?w=800&auto=format&fit=crop&q=80',
    top_review = 'Great vibes, live music is amazing. Young, fun crowd every weekend.',
    top_review_author = 'Youssef A.'
WHERE id = 'kinobar';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1510812431401-41d2bd2722f3?w=800&auto=format&fit=crop&q=80',
    top_review = 'Authentic Spanish tapas. Wine list is impressive. Saturday nights are legendary.',
    top_review_author = 'Maria F.'
WHERE id = 'la_bodega';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1572116469696-31de0f17cc34?w=800&auto=format&fit=crop&q=80',
    top_review = 'Cozy hidden gem. Creative cocktails and laid-back vibe.',
    top_review_author = 'Said B.'
WHERE id = 'le_trica';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1571266028243-d220c6a45c89?w=800&auto=format&fit=crop&q=80',
    top_review = 'Best techno nights in town. International DJs every weekend.',
    top_review_author = 'DJ Karim'
WHERE id = 'bla_bla';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1597212849308-c87f9bb73fa2?w=800&auto=format&fit=crop&q=80',
    top_review = 'Absolutely breathtaking. The architecture is incredible inside and out.',
    top_review_author = 'Anna K.'
WHERE id = 'hassan_ii_mosque';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1518509562904-e7ef99cddc85?w=800&auto=format&fit=crop&q=80',
    top_review = 'Best place for sunset walks. Tons of cafes and restaurants along the way.',
    top_review_author = 'Ahmed J.'
WHERE id = 'corniche_ain_diab';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1547636780-4b54bd3aa54e?w=800&auto=format&fit=crop&q=80',
    top_review = 'Get lost in the colorful streets! Great place to buy traditional crafts.',
    top_review_author = 'Linda T.'
WHERE id = 'old_medina';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1561214115-f2f134cc4912?w=800&auto=format&fit=crop&q=80',
    top_review = 'Inspiring contemporary art exhibitions. A must for art lovers.',
    top_review_author = 'Camille O.'
WHERE id = 'villa_des_arts';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1548013146-72479768bada?w=800&auto=format&fit=crop&q=80',
    top_review = 'Beautiful architecture with a unique history. Worth a visit.',
    top_review_author = 'Marc D.'
WHERE id = 'cathedrale_sacre_coeur';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1583268426913-eb1e5915e8e7?w=800&auto=format&fit=crop&q=80',
    top_review = 'Modern waterfront with great restaurants. Perfect for evening walks.',
    top_review_author = 'Khalid M.'
WHERE id = 'casablanca_marina';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1539020140153-e479b8c91b87?w=800&auto=format&fit=crop&q=80',
    top_review = 'Hidden gem! The cedar ceilings are absolutely stunning.',
    top_review_author = 'Rachid F.'
WHERE id = 'mahkama_du_pacha';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1530549387789-4c1017266635?w=800&auto=format&fit=crop&q=80',
    top_review = 'Tons of slides and pools! Perfect for kids and adults alike.',
    top_review_author = 'Amal Z.'
WHERE id = 'tamaris_aquaparc';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1572636356942-7c66c4b6e8c0?w=800&auto=format&fit=crop&q=80',
    top_review = 'Beautiful green space in the city center. Great for morning runs.',
    top_review_author = 'Ibrahim S.'
WHERE id = 'parc_de_la_ligue_arabe';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=800&auto=format&fit=crop&q=80',
    top_review = 'Well-maintained modern park. Kids love the playgrounds.',
    top_review_author = 'Salma R.'
WHERE id = 'anfa_park';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1583244532610-2a234c8b81e9?w=800&auto=format&fit=crop&q=80',
    top_review = 'Great rides with ocean view. Fun for the whole family.',
    top_review_author = 'Hamza K.'
WHERE id = 'sindibad_beach_resort';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1519567241046-7f570eee3ce6?w=800&auto=format&fit=crop&q=80',
    top_review = 'Biggest mall in Africa! Tons of shops, aquarium is amazing.',
    top_review_author = 'Zineb O.'
WHERE id = 'morocco_mall';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1481437156560-3205f6a55735?w=800&auto=format&fit=crop&q=80',
    top_review = 'Upscale shopping by the beach. Lovely views from the cafes.',
    top_review_author = 'Bilal H.'
WHERE id = 'anfa_place';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1488459716781-31db52582fe9?w=800&auto=format&fit=crop&q=80',
    top_review = 'Authentic local market. Fresh seafood at the best prices.',
    top_review_author = 'Latifa Q.'
WHERE id = 'marche_central';

UPDATE public.venues SET
    image_url = 'https://images.unsplash.com/photo-1483653364400-eedcfb9f1f88?w=800&auto=format&fit=crop&q=80',
    top_review = 'Iconic towers in the heart of the city. Great shopping mix.',
    top_review_author = 'Sami T.'
WHERE id = 'twin_center';
