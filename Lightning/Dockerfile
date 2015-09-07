FROM localhost:5000/base

RUN apt-get install -qy git

RUN git clone https://github.com/lightning-viz/lightning.git /lightning \
    && cd /lightning \
    && npm install \
    && npm run build

EXPOSE 3000

WORKDIR "/lightning"

CMD ["npm","start"]
