'use strict';
var _ = require('lodash');

module.exports = {

    image: function(imageData) {
        return {
            images: _.isArray(imageData) ? imageData : [imageData]
        };
    },

    gallery: function(imageData) {
        return {
            images: _.isArray(imageData) ? imageData : [imageData]
        };
    }

};

