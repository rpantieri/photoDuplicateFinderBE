package com.rpantieri.photoDuplicateFinder.resources;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;

import com.rpantieri.photoDuplicateFinder.dto.DuplicateDTO;
import com.rpantieri.photoDuplicateFinder.utils.BiObjectHolder;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@RequestScoped
public class ImagesCopiesHolder {
    private static final Logger LOG = Logger.getLogger(ImagesCopiesHolder.class);

    @ConfigProperty(name = "photoDuplicateFinder.folderOrder")
    List<String> folderOrder;

    @ConfigProperty(name = "photoDuplicateFinder.rootDir")
    String rootDir;

    List<String> alwaysKeep;

    private String md5;
    private String sha;
    private BigInteger size;

    private List<BiObjectHolder<String, Long>> files = new ArrayList<>();

    public ImagesCopiesHolder(@ConfigProperty(name = "photoDuplicateFinder.alwaysKeepFolder") List<String> value,
            @ConfigProperty(name = "photoDuplicateFinder.rootDir") String root) {
        super();
        this.alwaysKeep = value.stream().map((x) -> root + "\\" + x).collect(Collectors.toList());
    }

    public boolean canAdd(String fileName, String md5Val, String shaVal, BigInteger sizeVal, BigInteger id) {
        if (md5 == null) {
            this.md5 = md5Val;
            this.sha = shaVal;
            this.size = sizeVal;
        }
        if (!md5.equals(md5Val) || !sha.equals(shaVal) || !size.equals(sizeVal))
            return false;
        for (String s : alwaysKeep)
            if (fileName.startsWith(s)) {
                LOG.info("skipping file " + fileName);
                return false;
            }

        this.files.add(new BiObjectHolder<String, Long>(fileName, id.longValue()));
        return true;
    }

    public void reinit(String fileName, String md5Val, String shaVal, BigInteger sizeVal, BigInteger id) {
        this.md5 = md5Val;
        this.sha = shaVal;
        this.size = sizeVal;
        this.files.clear();
        for (String s : alwaysKeep)
            if (fileName.startsWith(s)) {
                LOG.info("skipping file " + fileName);
                return;
            }
        this.files.add(new BiObjectHolder<String, Long>(fileName, id.longValue()));
    }

    public void writeResult(List<DuplicateDTO> ll) {
        Collections.sort(files, this::compare);
        boolean delete = false;
        for (BiObjectHolder<String, Long> s : files) {
            if (delete) {
                ll.add(new DuplicateDTO(s.getFirst(), false, s.getSecond()));
            } else {
                delete = true;
                ll.add(new DuplicateDTO(s.getFirst(), true, s.getSecond()));
            }
        }
    }

    public int size() {
        return files.size();
    }

    public int compare(BiObjectHolder<String, Long> fileObj1, BiObjectHolder<String, Long> fileObj2) {
        int s = rootDir.length();
        String file1 = fileObj1.getFirst();
        String file2 = fileObj2.getFirst();
        if (file1.length() > s)
            file1 = file1.substring(s + 1);
        if (file1.indexOf('\\') > 0)
            file1 = file1.substring(0, file1.lastIndexOf('\\'));
        if (file2.length() > s)
            file2 = file2.substring(s + 1);
        if (file2.indexOf('\\') > 0)
            file2 = file2.substring(0, file2.lastIndexOf('\\'));
        int i1 = folderOrder.indexOf(file1);
        int i2 = folderOrder.indexOf(file2);
        if (i1 < 0)
            i1 = 100;
        if (i2 < 0)
            i2 = 100;
        return i1 < i2 ? -1 : (i1 == i2 ? 0 : 1);
    }

}