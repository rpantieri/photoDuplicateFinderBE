package com.rpantieri.photoDuplicateFinder.bo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.function.BiPredicate;

import javax.enterprise.context.ApplicationScoped;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Image;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;

import com.rpantieri.photoDuplicateFinder.model.ImageInfo;
import com.rpantieri.photoDuplicateFinder.model.ImageInfo_;
import com.rpantieri.photoDuplicateFinder.model.ImageThumbnail;
import com.rpantieri.photoDuplicateFinder.providers.ImageInfoProvider;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.engine.jdbc.BlobProxy;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;

@ApplicationScoped
public class ImageScanner {

	private static BiPredicate<Path, BasicFileAttributes> matcher = null;

	private static MessageDigest md5Digest = null;
	private static MessageDigest shaDigest = null;

	@Inject
	EntityManager em;

	@ConfigProperty(name = "photoDuplicateFinder.rootDir")
	String rootDir;

	@Inject
	ImageInfoProvider infoProvider;

	static {
		matcher = (p, bfa) -> bfa.isRegularFile();

		try {
			md5Digest = MessageDigest.getInstance("MD5");
			shaDigest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace(System.out);
		}
	}

	// @Scheduled(every = "300s", concurrentExecution = ConcurrentExecution.SKIP)
	public void start() {
		synchronized (mutex) {
			if (running)
				return;
			run();
		}

	}

	private Object mutex = new Object();
	private boolean running = false;

	public void run() {

		try {
			Files.find(Paths.get(rootDir), 999, matcher).forEach(this::persistFile);
			;
		} catch (IOException e) {
			e.printStackTrace(System.out);
		}
	}

	@Transactional
	public void persistFile(Path filePath) {
		System.out.println("***");
		try {
			File f = filePath.toFile();
			BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<ImageInfo> query = cb.createQuery(ImageInfo.class);
			Root<ImageInfo> root = query.from(ImageInfo.class);
			query.where(cb.equal(root.get(ImageInfo_.imagePath), f.getAbsolutePath()));
			query.select(root);
			List<ImageInfo> ll = em.createQuery(query).getResultList();
			ImageInfo i = ll.size() > 0 ? ll.get(0) : null;
			System.out.println("Checking file:" + f.getAbsolutePath());
			if (i != null) {
				System.out.println("Existing record skip");
			} else {
				i = new ImageInfo();
				System.out.println("Creating new record");
			}
			if (i.getImagePath() == null)
				i.setImagePath(f.getAbsolutePath());
			if (i.getFileName() == null)
				i.setFileName(f.getName().toLowerCase());
			if (i.getSize() == null)
				i.setSize(f.length());
			if (i.getMd5() == null)
				i.setMd5(getFileChecksum(md5Digest, f));
			if (i.getSha1() == null)
				i.setSha1(getFileChecksum(shaDigest, f));
			if (i.getCreationDate() == null)
				i.setCreationDate(new Date(attr.creationTime().toMillis()));
			em.persist(i);
			System.out.println("File name:" + i.getFileName());
			System.out.println("File size:" + i.getSize());
			System.out.println("File Md%:" + i.getMd5());
			System.out.println("File SHA1:" + i.getSha1());
			System.out.println("File creationdate:" + i.getCreationDate());
			if (i.getThumbnail() == null) {
				System.out.println("Generating thumbnail");
				ImageThumbnail th = new ImageThumbnail();
				th.setImageInfo(i);
				th.setContent200(BlobProxy.generateProxy(createThumbnail(i.getImagePath(), 200)));
				em.persist(th);
				i.setThumbnail(th);
				em.persist(i);
			}
			em.flush();
			System.out.println("***");
		} catch (

		Exception e) {
			e.printStackTrace(System.out);
		} finally {

		}
	}

	public static byte[] createThumbnail(String path, int maxSize) {
		try {
			BufferedImage sourceImg = ImageIO.read(new File(path));
			if (sourceImg == null)
				return null;
			int sourceW = sourceImg.getWidth();
			int sourceH = sourceImg.getHeight();
			boolean maxIsW = sourceW > sourceH;
			int maxS = maxIsW ? sourceW : sourceH;
			double ratio = maxS > maxSize ? (double) maxSize / (double) maxS : 1;
			int thumnail_width = (int) (sourceW * ratio);
			int thumbnail_height = (int) (sourceH * ratio);
			BufferedImage img = new java.awt.image.BufferedImage(thumnail_width, thumbnail_height,
					BufferedImage.TYPE_INT_RGB);
			img.createGraphics().drawImage(
					sourceImg.getScaledInstance(thumnail_width, thumbnail_height, Image.SCALE_SMOOTH), 0, 0, null);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ImageIO.write(img, "jpg", bos);
			bos.flush();
			byte[] data = bos.toByteArray();
			return data;
		} catch (IOException e) {
			System.out.println("Exception while generating thumbnail " + e.getMessage());
			return null;
		}
	}

	private static String getFileChecksum(MessageDigest digest, File file) throws IOException {
		// Get file input stream for reading the file content
		FileInputStream fis = new FileInputStream(file);

		// Create byte array to read data in chunks
		byte[] byteArray = new byte[1024];
		int bytesCount = 0;

		// Read file data and update in message digest
		while ((bytesCount = fis.read(byteArray)) != -1) {
			digest.update(byteArray, 0, bytesCount);
		}
		;

		// close the stream; We don't need it now.
		fis.close();

		// Get the hash's bytes
		byte[] bytes = digest.digest();

		// This bytes[] has bytes in decimal format;
		// Convert it to hexadecimal format
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
		}

		// return complete hash
		return sb.toString();
	}
}
