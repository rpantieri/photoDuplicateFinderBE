package com.rpantieri.photoDuplicateFinder.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.rpantieri.photoDuplicateFinder.model.ImageInfo;
import com.rpantieri.photoDuplicateFinder.model.ImageThumbnail;

import org.jboss.logging.Logger;

@Path("/image")
@ApplicationScoped
public class ImageResource {

    private static final Logger LOG = Logger.getLogger(ImageResource.class);

    @Inject
    EntityManager em;

    @Path("/{id}/thumbnail")
    @GET
    @Produces(value = MediaType.APPLICATION_OCTET_STREAM)
    @Transactional
    public Response thumbnail(@PathParam(value = "id") Long id) throws SQLException, IOException {
        ImageInfo i = em.find(ImageInfo.class, id);
        ImageThumbnail t = i.getThumbnail();
        Blob b = t.getContent200();
        File tempFile = File.createTempFile("photoDuplicateFinder", i.getFileName(), null);
        FileOutputStream fos = new FileOutputStream(tempFile);
        byte[] bb = new byte[1024];
        int read = 0;
        InputStream inStream = b.getBinaryStream();
        while ((read = inStream.read(bb, 0, 1024)) > 0) {
            fos.write(bb, 0, read);
        }

        fos.flush();
        fos.close();
        return Response.ok(new FileInputStream(tempFile), MediaType.APPLICATION_OCTET_STREAM)
                .header("content-disposition", "attachment; filename=" + i.getFileName())
                .header("Content-Length", i.getSize()).build();
    }

    @Path("/{id}")
    @GET
    @Produces(value = MediaType.APPLICATION_OCTET_STREAM)
    @Transactional
    public Response image(@PathParam(value = "id") Long id) throws SQLException, FileNotFoundException {
        ImageInfo i = em.find(ImageInfo.class, id);
        return Response.ok(new FileInputStream(i.getImagePath()), MediaType.APPLICATION_OCTET_STREAM)
                .header("content-disposition", "attachment; filename=" + i.getFileName())
                .header("Content-Length", i.getSize()).build();
    }

    @Path("/deletelist")
    @POST
    @Transactional(value = TxType.REQUIRES_NEW)
    @Consumes(value = MediaType.APPLICATION_JSON)
    public Response deletelist(List<Long> ids) {
        LOG.info("delete list begin");
        for (Long id : ids) {
            ImageInfo i = em.find(ImageInfo.class, id);
            File f = new File(i.getImagePath());
            LOG.info("delete list removing entity");
            if (i.getThumbnail() != null)
                em.remove(i.getThumbnail());
            em.remove(i);
            if (f.exists()) {
                LOG.info("delete list removing file");
                f.delete();
            }
            em.flush();
            em.clear();
        }
        LOG.info("delete list end");
        return Response.ok().build();
    }
}
