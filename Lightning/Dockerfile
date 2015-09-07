FROM localhost:5000/base

RUN git clone git@github.com:lightning-viz/lightning.git \
    && cd lightning \
    && npm install

EXPOSE 3000

CMD ["npm","start"]
