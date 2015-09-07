FROM ubuntu:latest

RUN apt-get update && apt-get install -qy nodejs npm git && ln -s /usr/bin/nodejs /usr/bin/node

RUN git clone https://github.com/lightning-viz/lightning.git /lightning \
    && cd /lightning \
    && npm install \
    && npm run build

EXPOSE 3000

WORKDIR "/lightning"

CMD ["npm","start"]
