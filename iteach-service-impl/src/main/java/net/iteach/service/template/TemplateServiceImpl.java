package net.iteach.service.template;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;

import net.iteach.api.TemplateService;
import net.iteach.api.model.TemplateModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@Service
public class TemplateServiceImpl implements TemplateService {

	private final Configuration configuration;

	@Autowired
	public TemplateServiceImpl(@Qualifier("templating") Configuration configuration) {
		this.configuration = configuration;
	}

	@Override
	public String generate(String templateId, Locale locale, TemplateModel templateModel) {
		// Creates the model as a map
		Map<String, Object> root = templateModel.toMap();
		// Gets the template
		Template template;
		try {
			template = configuration.getTemplate(templateId, locale, "UTF-8");
		} catch (IOException ex) {
			throw new TemplateNotFoundException (templateId, ex);
		}
		// Merging w/ the model
		StringWriter writer = new StringWriter();
		try {
			template.process(root, writer);
		} catch (TemplateException | IOException ex) {
			throw new TemplateMergeException (templateId, ex);
		}
		// OK
		return writer.toString();
	}

}
