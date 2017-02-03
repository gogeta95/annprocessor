package com.example;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.inject.Inject;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.JavaFileObject;

import dagger.Component;
import dagger.Provides;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({"dagger.Module", "dagger.Provides", "javax.inject.Inject", "dagger.Component"})
public final class DaggerBuilder extends AbstractProcessor {

    private Set<Pair<Element, ExecutableElement>> injectContructors = new HashSet<>();
    private Set<Pair<Element, VariableElement>> injectVariables = new HashSet<>();
    private Set<Pair<Element, ExecutableElement>> providers = new HashSet<>();
    private Set<Pair<Element, List<Element>>> components = new HashSet<>();

    @Override
    public boolean process(
            Set<? extends TypeElement> annotations,
            RoundEnvironment env) {
        try {
            Collection<? extends Element> allElements =
                    env.getRootElements();

            populateInjectors(allElements);
            populateProviders(env);

            populateComponents(env);


            System.err.println("injectContructors\n");
            for (Pair<Element, ExecutableElement> pair : injectContructors) {
                printPair(pair);
            }

            System.err.println("injectVariables\n");
            for (Pair<Element, VariableElement> pair : injectVariables) {
                printPair(pair);
            }

            System.err.println("providers\n");
            for (Pair<Element, ExecutableElement> pair : providers) {
                printPair(pair);
            }

            System.err.println("components\n");
            for (Pair<Element, List<Element>> pair : components) {
                System.out.println(pair.getFirst());
                for (Element element:pair.getSecond())
                System.out.println("    "+element);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;

    }

    private void printPair(Pair<Element, ? extends Element> pair) {
        System.out.println(pair.getFirst() + " : " + pair.getSecond());
    }

    private void populateComponents(RoundEnvironment env) {
        Collection<? extends Element> elements = env.getElementsAnnotatedWith(Component.class);
        populateComponents(elements);
    }

    private void populateComponents(Collection<? extends Element> elements) {
        for (Element component : elements) {
            Collection<? extends Element> elementList = component.getEnclosedElements();
            List<Element> reqElements = new ArrayList<>();
            for (Element element : elementList) {
                if (element.getKind() == ElementKind.METHOD) {
                    reqElements.add(element);
                }
            }
            components.add(new Pair<>(component, reqElements));
        }
    }

    private void populateProviders(RoundEnvironment env) {
        Collection<? extends Element> elements = env.getElementsAnnotatedWith(Provides.class);
        populateProviders(elements);
    }

    private void populateInjectors(Collection<? extends Element> allElements) {
        for (Element element : allElements) {
            if (element.getAnnotation(Inject.class) != null) {
                if (element instanceof VariableElement) {
                    VariableElement variableElement = (VariableElement) element;
                    injectVariables.add(new Pair<>(element.getEnclosingElement(), variableElement));
                } else if (element instanceof ExecutableElement && element.getKind() == ElementKind.CONSTRUCTOR) {
                    ExecutableElement executableElement = (ExecutableElement) element;
                    injectContructors.add(new Pair<>(element.getEnclosingElement(), executableElement));
                }
            } else {
                populateInjectors(element.getEnclosedElements());
            }
        }
    }

    private String generateSource(TypeElement type, String className) {
        return "";
    }

    private void populateProviders(Collection<? extends Element> elements) {
        for (Element element : elements) {
            if (element instanceof ExecutableElement) {
                ExecutableElement executableElement = (ExecutableElement) element;
                providers.add(new Pair<>(executableElement.getEnclosingElement(), executableElement));
            }
        }
    }

    private String generatedSubclassName(TypeElement type) {
        System.out.println(type.getSimpleName());
        return "Inject_" + type.getSimpleName();
    }

    private void writeSourceFile(
            String className,
            String text,
            TypeElement originatingType) {
        try {
            JavaFileObject sourceFile =
                    processingEnv.getFiler().
                            createSourceFile(className, originatingType);
            Writer writer = sourceFile.openWriter();
            try {
                writer.write(text);
            } finally {
                writer.close();
            }
        } catch (IOException e) {// silent}
        }
    }
}
