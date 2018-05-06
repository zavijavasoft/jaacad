package com.zavijavasoft.jaacad;


import org.junit.Test;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.Date;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;


public class PersistenceManagerTest {


    @Test
    public void saveLoadTest() {

        File fl = new File("src/test/res");

        PersistenceManager manager = new PersistenceManager();

        ZonedDateTime zdt = ZonedDateTime.now().minusDays(2);

        GalleryEntity ge1 = new GalleryEntity();
        ge1.setResourceId("1");
        ge1.setState(GalleryEntity.State.IMAGE);
        ge1.setPathToImage("aaaaaa"); //Not Empty
        ge1.setLoadedDateTime(Date.from(zdt.toInstant()));

        GalleryEntity ge2 = new GalleryEntity();
        ge2.setResourceId("2");
        ge2.setState(GalleryEntity.State.IMAGE);


        GalleryEntity ge3 = new GalleryEntity();
        ge3.setResourceId("3");
        ge3.setState(GalleryEntity.State.IMAGE);

        manager.getEntities().add(ge1);
        manager.getEntities().add(ge2);
        manager.getEntities().add(ge3);

        assertNotNull(manager.findEntityById("2"));
        ge2.setMarkedAsDead(true);
        assertNull(manager.findEntityById("2"));

        manager.saveCachedGallery(true, fl.getAbsoluteFile());
        manager.loadCachedGallery(true, fl.getAbsoluteFile());

        assertEquals(manager.getEntities().size(), 2);
        assertNull(manager.findEntityById("2"));
        GalleryEntity ge1r = manager.findEntityById("1");
        assertNotNull(ge1r);
        assertEquals(ge1r.getState(), GalleryEntity.State.THUMBNAIL);

        manager.saveCachedGallery(true, fl.getAbsoluteFile());
        manager.loadCachedGallery(false, fl.getAbsoluteFile());

        assertEquals(manager.getEntities().size(), 0);
        File flJson = new File(fl.getAbsoluteFile(), PersistenceManager.JAACAD_GALLERY_JSON);
        flJson.delete();
    }
}
