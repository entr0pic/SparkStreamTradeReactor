FROM localhost:5000/base

RUN apt-get -y install git

RUN git clone https://github.com/lightning-viz/lightning.git \
    && cd lightning \
    && npm install

EXPOSE 3000

CMD ["npm","start"]
