# Modelos de IA de Atelier

En Atelier usamos IA para ayudar a diseñar prendas, revisar tendencias y ver si una idea se puede producir con los materiales disponibles. Para estas tareas elegimos modelos de la familia Qwen.

## ¿Qué tuvimos en cuenta?

Buscamos modelos que:
- Se puedan usar sin pagar por cada consulta ni contratar una suscripción.
- Tengan una licencia abierta que permita usarlos también en proyectos comerciales.
- Entiendan bien el español, porque ese es el idioma de nuestros usuarios.

## ¿Por qué elegimos Qwen?

Preferimos usar una sola familia de modelos para que la integración sea más sencilla. Así trabajamos con herramientas compatibles entre sí y tenemos menos componentes que mantener. Además, el modelo que entiende las descripciones puede ayudar a mejorar las instrucciones para generar imágenes y luego analizar los resultados.

Si el servidor no tiene suficiente capacidad para generar imágenes con Qwen-Image, podemos evaluar FLUX.1 [schnell] como alternativa.

## Modelos que planeamos usar

| Módulo | Modelo | Licencia | Uso en Atelier |
|---|---|---|---|
| Diseño generativo | Qwen-Image / Qwen-Image-Edit | Apache 2.0 | Crear imágenes de prendas y hacer cambios en ellas |
| Tendencias | Qwen3.6-35B-A3B | Apache 2.0 | Revisar imágenes y reconocer colores, cortes, telas y estampados |
| Viabilidad e inventario | Qwen3.6-35B-A3B | Apache 2.0 | Comparar una prenda con el stock y sugerir alternativas |
| Embeddings (opcional) | Qwen3-Embedding | Apache 2.0 | Encontrar prendas y tendencias parecidas |

## ¿Qué aporta cada modelo?

### Qwen3.6-35B-A3B: análisis de texto e imágenes

Este modelo puede trabajar con imágenes y texto, por lo que nos sirve para revisar tendencias y apoyar el análisis de viabilidad. También puede solicitar información del inventario a través de Spring Boot; de esa manera, las recomendaciones pueden basarse en el stock real.

Su licencia es Apache 2.0. El modelo tiene 35 mil millones de parámetros en total, aunque activa alrededor de 3 mil millones para cada parte del proceso, lo que ayuda a que responda con rapidez para su tamaño.

### Qwen-Image y Qwen-Image-Edit: creación y edición de imágenes

Qwen3.6 trabaja con texto e imágenes, pero no genera imágenes. Por eso elegimos Qwen-Image para crear propuestas de prendas y Qwen-Image-Edit para modificar imágenes existentes sin tener que empezar de nuevo. Ambos modelos son de la familia Qwen y tienen licencia Apache 2.0.

## Opciones que no elegimos

| Modelo o servicio | Motivo |
|---|---|
| FLUX.1 [schnell] | Lo dejamos como alternativa si Qwen-Image no puede ejecutarse en el servidor |
| FLUX.1 [dev] | Su licencia no permite el uso comercial |
| Stable Diffusion 3.5 | Su licencia comunitaria tiene condiciones adicionales |
| Llama / Gemma | Tienen licencias propias con restricciones de uso |
| APIs de pago (GPT, Gemini, Midjourney, etc.) | No cumplen con nuestro criterio de uso local y sin costo; además, enviarían datos a servicios externos |

## Así se usaría la IA en Atelier

1. El usuario describe una prenda desde la web o la aplicación.
2. Qwen3.6 ayuda a completar la descripción para que sirva como instrucción para generar imágenes.
3. Qwen-Image crea distintas propuestas de la prenda.
4. Qwen3.6 identifica los materiales necesarios y Spring Boot revisa si hay stock.
5. Con esa información, Qwen3.6 explica si se puede producir la prenda y qué alternativas hay.
