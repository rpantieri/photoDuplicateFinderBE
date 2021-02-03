package com.rpantieri.photoDuplicateFinder.resources;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.rpantieri.photoDuplicateFinder.bo.ImageScanner;
import com.rpantieri.photoDuplicateFinder.dto.DuplicateDTO;

@Path("/utils")
public class UtilsResource {

    @Inject
    EntityManager em;

    @Inject
    ImageScanner scanner;

    @Inject
    ImagesCopiesHolder holder;

    @GET
    @Path("duplicatelist")
    @Transactional
    @Produces(value = MediaType.APPLICATION_JSON)
    public List<DuplicateDTO> getDuplicateList(@DefaultValue("49") @QueryParam("pageSize") int pageSize) {
        ArrayList<DuplicateDTO> ll = new ArrayList<>();

        em.createNativeQuery("select imagepath, md5, sha1, size, id from image_info order by md5  ").getResultStream()
                .forEach((Object ob) -> {
                    Object[] row = (Object[]) ob;
                    if (!holder.canAdd((String) row[0], (String) row[1], (String) row[2], (BigInteger) row[3],
                            (BigInteger) row[4])) {
                        if (holder.size() > 1) {
                            holder.writeResult(ll);
                        }
                        holder.reinit((String) row[0], (String) row[1], (String) row[2], (BigInteger) row[3],
                                (BigInteger) row[4]);
                    }

                });
        if (holder.size() > 1) {
            holder.writeResult(ll);
        }
        return ll.size() < pageSize ? ll : ll.subList(0, pageSize - 1);
    }

}
