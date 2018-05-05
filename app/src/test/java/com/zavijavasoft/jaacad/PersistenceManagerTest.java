package com.zavijavasoft.jaacad;

import org.testng.annotations.Test;

import java.io.File;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

public class PersistenceManagerTest {

    @Test
    void saveLoadTest() {

        File fl = new File("");
        String s = fl.getAbsolutePath();
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
        ge2.setMarkedAsDead(true);
        ge2.setLoadedDateTime(Date.from(zdt.toInstant()));

        GalleryEntity ge3 = new GalleryEntity();
        ge3.setResourceId("2");
        ge3.setState(GalleryEntity.State.IMAGE);
        ge3.setLoadedDateTime(Date.from(zdt.toInstant()));

        manager.getEntities().add(ge1);
        manager.getEntities().add(ge2);
        manager.getEntities().add(ge3);

        assertNull(manager.findEntityById("2"));

        manager.saveCachedGallery(true, new File(""));
        manager.loadCachedGallery(true, new File(""));

        assertEquals(manager.getEntities().size(), 2);
        assertNull(manager.findEntityById("2"));
        GalleryEntity ge1r = manager.findEntityById("1");
        assertNotNull(ge1r);
        assertEquals(ge1r.getState(), GalleryEntity.State.THUMBNAIL);

        manager.saveCachedGallery(true, new File(""));
        manager.loadCachedGallery(false, new File(""));

        assertEquals(manager.getEntities().size(), 0);
    }
}
