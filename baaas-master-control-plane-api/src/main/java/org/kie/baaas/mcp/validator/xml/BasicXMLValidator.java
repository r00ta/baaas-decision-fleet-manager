/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.baaas.mcp.validator.xml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.helpers.DefaultHandler;

public class BasicXMLValidator implements ConstraintValidator<BasicXML, String> {

    private String xmlContent;

    @Override
    public void initialize(BasicXML constraintAnnotation) {
        this.xmlContent = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {

        if (null == s) {
            return true;
        }

        try {
            SAXParserFactory fac = SAXParserFactory.newInstance();
            SAXParser parser = fac.newSAXParser();
            // default handler, no special validation here
            DefaultHandler handler = new DefaultHandler();
            // Transform the input string into InputStream
            InputStream is = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
            // try to parse
            parser.parse(is, handler);
        } catch (final Exception e) {
            // in case of any exception, fail it.
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate(e.getMessage()).addConstraintViolation();
            return false;
        }

        return true;
    }
}
