(function ($) {

    function populate() {
        var hostname = location.host;
        $.getJSON("http://" + hostname + "/books", function (data) {
            $.each(data, function (key, val) {
                if (val.smallBookCover != null) {
                    $('#items').append("<li class='span2'><a class='thumbnail'><img src='" + val.smallBookCover + "'/></a></li>");
                } else {
                    $('#items').append("<li class='span2'><a class='thumbnail'><img src='../../../assets/img/image-not-found.jpg'/></a></li>");
                }
            });
        });
    };

    // Populate items
    populate();

}(jQuery));