FROM adoptopenjdk/openjdk11:debian-slim

WORKDIR /app

COPY build/distributions/adventure-webui.tar /app/
RUN tar -xvf /app/adventure-webui.tar
RUN rm /app/adventure-webui.tar

ENTRYPOINT ["sh", "/app/adventure-webui/bin/adventure-webui"]
