var books = (function ($) {

    return function () {
        var hostname = location.host;
        $.getJSON("http://" + hostname + "/books", function (data) {
            var items = [];
            $.each(data, function (key, val) {
                if (val.smallBookCover != null) {
                    items.push("<div class='span3'><img class='thumbnail' src='" + val.smallBookCover + "'/></div>")
                } else {
                    items.push("<div class='span3'><img class='thumbnail' src='../../../assets/img/image-not-found.jpg'/></div>")
                }
            });
            function createRow(index) {
                var row = $('<div />', {
                    id      : "book-row-" + index,
                    'class' : "row"
                });
                $("#books-div").append(row);
                return row;
            }
            var currentRow = createRow(0);
            var count = 0;
            items.map(function (item) {
                count++;
                currentRow.append(item);
                if (count % 4 == 0 && count < items.length) {
                    currentRow = createRow(count) ;
                }
            });
        });
    };
}(jQuery));