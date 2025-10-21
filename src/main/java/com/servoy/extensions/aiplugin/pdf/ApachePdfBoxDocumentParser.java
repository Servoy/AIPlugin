package com.servoy.extensions.aiplugin.pdf;

import dev.langchain4j.data.document.BlankDocumentException;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.Metadata;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;

import com.servoy.j2db.plugins.IFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static dev.langchain4j.internal.Utils.isNullOrBlank;

/**
 * Parses PDF file into a {@link Document} using Apache PDFBox library
 */
public class ApachePdfBoxDocumentParser implements DocumentParser {

	private final boolean includeMetadata;

	public ApachePdfBoxDocumentParser() {
		this(false);
	}

	public ApachePdfBoxDocumentParser(boolean includeMetadata) {
		this.includeMetadata = includeMetadata;
	}

	public Document parse(Object source) {
		if (source instanceof InputStream is) {
			return parse(is);
		} else if (source instanceof byte[] bytes) {
			return parse(new java.io.ByteArrayInputStream(bytes));
		} else if (source instanceof String filename) {
			try (FileInputStream fis = new FileInputStream(filename)) {
				Document document = parse(fis);
				document.metadata().put("filename", filename);
				return document;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else if (source instanceof File file) {
			try (FileInputStream fis = new FileInputStream(file)) {
				Document document = parse(fis);
				document.metadata().put("filename", file.getName());
				return document;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else if (source instanceof IFile file) {
			try (InputStream is = file.getInputStream()) {
				Document document = parse(is);
				if(file.getFile() != null )document.metadata().put("filename", file.getFile().getName());
//				document.metadata().put("filename", file.getName());
				return document;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		else {
			throw new IllegalArgumentException("Unsupported source type: " + source.getClass().getName());
		}
	}

	@Override
	public Document parse(InputStream inputStream) {
		try (PDDocument pdfDocument = Loader.loadPDF(new RandomAccessReadBuffer(inputStream))) {
			PDFTextStripper stripper = new PDFTextStripper();
			String text = stripper.getText(pdfDocument);
			if (isNullOrBlank(text)) {
				throw new BlankDocumentException();
			}
			return includeMetadata ? Document.from(text, toMetadata(pdfDocument)) : Document.from(text);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Metadata toMetadata(PDDocument pdDocument) {
		PDDocumentInformation documentInformation = pdDocument.getDocumentInformation();
		Metadata metadata = new Metadata();
		for (String metadataKey : documentInformation.getMetadataKeys()) {
			String value = documentInformation.getCustomMetadataValue(metadataKey);
			if (value != null)
				metadata.put(metadataKey, value);
		}
		return metadata;
	}
}