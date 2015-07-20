FROM localhost:5000/base

#RUN apk --update add nodejs make g++

COPY ./app /
COPY ./package.json /package.json

RUN npm install

CMD ["node", "/TradeGenerator.js"]
