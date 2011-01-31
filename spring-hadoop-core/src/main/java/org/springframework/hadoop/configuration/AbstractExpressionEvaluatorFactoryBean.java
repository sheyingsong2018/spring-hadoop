/*
 * Copyright 2006-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.hadoop.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.hadoop.util.BeanFactoryConversionService;
import org.springframework.hadoop.util.reflect.MethodUtils;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 * 
 */
public abstract class AbstractExpressionEvaluatorFactoryBean<T> implements FactoryBean<T>, InitializingBean,
		BeanFactoryAware {

	private Object target;

	private String method;

	private ConversionService conversionService = ConversionServiceFactory.createDefaultConversionService();

	private Class<? extends Writable> outputKeyType;

	private Class<? extends Writable> outputValueType;

	public void setTarget(Object target) {
		this.target = target;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public void setOutputKeyType(Class<? extends Writable> outputKeyType) {
		this.outputKeyType = outputKeyType;
	}

	public void setOutputValueType(Class<? extends Writable> outputValueType) {
		this.outputValueType = outputValueType;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		BeanFactoryConversionService conversionService = new BeanFactoryConversionService();
		conversionService.setBeanFactory(beanFactory);
		this.conversionService = conversionService;
	}

	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.state(target != null, "Target must be provided");
		Assert.state(outputValueType != null, "Ouput value type must be provided");
		Assert.state(outputKeyType != null, "Ouput key type must be provided");
		addDefaultConverters();
	}

	public T getObject() throws Exception {
		if (getObjectType().isInstance(target)) {
			@SuppressWarnings("unchecked")
			T result = (T) target;
			return result;
		}
		if (method != null) {
			return doGetObject(target, method, conversionService, outputKeyType, outputValueType);
		}
		return doGetObject(target, conversionService, outputKeyType, outputValueType);
	}

	protected abstract T doGetObject(Object target, ConversionService conversionService,
			Class<? extends Writable> outputKeyType, Class<? extends Writable> outputValueType);

	protected abstract T doGetObject(Object target, String method, ConversionService conversionService,
			Class<? extends Writable> outputKeyType, Class<? extends Writable> outputValueType);

	public abstract Class<?> getObjectType();

	public boolean isSingleton() {
		return true;
	}

	private void addDefaultConverters() {
		if (!(conversionService instanceof ConverterRegistry)) {
			return;
		}
		ConverterRegistry registry = (ConverterRegistry) conversionService;
		Set<Converter<?, ?>> converters = new HashSet<Converter<?, ?>>();
		converters.add(new Converter<Iterable<?>, Collection<?>>() {
			public Collection<?> convert(Iterable<?> source) {
				ArrayList<Object> result = new ArrayList<Object>();
				for (Object item : source) {
					result.add(item);
				}
				return result;
			}
		});
		converters.add(new Converter<IntWritable, Integer>() {
			public Integer convert(IntWritable source) {
				return source.get();
			}
		});
		converters.add(new Converter<Integer, IntWritable>() {
			public IntWritable convert(Integer source) {
				return new IntWritable(source);
			}
		});
		converters.add(new Converter<Text, String>() {
			public String convert(Text source) {
				return source.toString();
			}
		});
		converters.add(new Converter<String, Text>() {
			public Text convert(String source) {
				return new Text(source);
			}
		});
		ConversionServiceFactory.registerConverters(converters, registry);
	}

}
