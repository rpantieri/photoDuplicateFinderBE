package com.rpantieri.photoDuplicateFinder.providers;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import com.rpantieri.photoDuplicateFinder.model.ImageInfo;
import com.rpantieri.photoDuplicateFinder.model.ImageThumbnail;

@ApplicationScoped
public class ImageInfoProvider {

    @Inject
    EntityManager em;

    @Transactional
    public void persist(ImageInfo value) {
        if (value.getId() != null)
            value = em.merge(value);
        em.persist(value);
        em.flush();
    }

    @Transactional
    public void persist(ImageThumbnail value) {
        if (value.getId() != null)
            value = em.merge(value);
        if (value.getImageInfo() != null) {
            value.setImageInfo(em.merge(value.getImageInfo()));
        }
        em.persist(value);
        em.flush();
    }
}