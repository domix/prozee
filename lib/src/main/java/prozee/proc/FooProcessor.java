package prozee.proc;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("prozee.proc.Foo")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class FooProcessor extends AbstractProcessor {
  String setterTemplate = """
        public %s %s(%s value) {
            object.%s(value);
            return this;
        }
    """;
  String classTemplate = """
    package %s;
        
    @javax.annotation.processing.Generated(value = "%s")
    public class %s {
        
        private %s object = new %s();
        
        public %s build() {
            return object;
        }
        
    %s
    }
    """;

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (TypeElement annotation : annotations) {
      Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);

      Map<Boolean, List<Element>> annotatedMethods = annotatedElements.stream().collect(Collectors.partitioningBy(element -> ((ExecutableType) element.asType()).getParameterTypes().size() == 1 && element.getSimpleName().toString().startsWith("set")));

      List<Element> setters = annotatedMethods.get(true);
      List<Element> otherMethods = annotatedMethods.get(false);

      otherMethods.forEach(element -> processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@BuilderProperty must be applied to a setXxx method with a single argument", element));

      if (setters.isEmpty()) {
        continue;
      }

      String className = ((TypeElement) setters.get(0).getEnclosingElement()).getQualifiedName().toString();

      Map<String, String> setterMap = setters.stream().collect(Collectors.toMap(setter -> setter.getSimpleName().toString(), setter -> ((ExecutableType) setter.asType()).getParameterTypes().get(0).toString()));

      try {
        writeBuilderFile(className, setterMap);
      } catch (IOException e) {
        e.printStackTrace();
      }

    }

    return true;
  }

  private void writeBuilderFile(String className, Map<String, String> setterMap) throws IOException {
    String packageName = null;
    int lastDot = className.lastIndexOf('.');
    if (lastDot > 0) {
      packageName = className.substring(0, lastDot);
    }

    String simpleClassName = className.substring(lastDot + 1);
    String builderClassName = className + "Builder";
    String builderSimpleClassName = builderClassName.substring(lastDot + 1);

    JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(builderClassName);
    try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
      String generatorClassName = this.getClass().getCanonicalName();
      String generatorDate = "";

      String setters = setterMap.entrySet().stream()
        .map(entry -> {
          String methodName = entry.getKey();
          String argumentType = entry.getValue();

          return setterTemplate.formatted(builderSimpleClassName, methodName, argumentType, methodName);
        })
        .collect(Collectors.joining());

      String formatted = classTemplate.formatted(packageName, generatorClassName, builderSimpleClassName, simpleClassName, simpleClassName, simpleClassName, setters);

      out.println(formatted);
    }
  }
}
