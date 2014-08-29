(function ($) {

    function createImage(bookJson) {
        if (bookJson.smallBookCover != null) {
            return "<div class='col-md-2'><a class='thumbnail'><img src='" + bookJson.smallBookCover + "'/></a></div>";
        } else {
            return "<div class='col-md-2'><a class='thumbnail'><img src='../../../assets/img/image-not-found.jpg'/></a></div>";
        }
    }

    function getDetailDListItem(bookJson, attribute) {
        if (bookJson[attribute] != null) {
            return "<dt>" + attribute + ":</dt><dd>" + bookJson[attribute] + "</dd>";
        } else {
            return "<dt>" + attribute + ":</dt><dd>not set</dd>";
        }
    }

    function createBookDetailPanel(bookJson) {
        var details = [];
        details.push(getDetailDListItem(bookJson, "title"));
        details.push(getDetailDListItem(bookJson, "authors"));
        details.push(getDetailDListItem(bookJson, "isbn"));
        var dl = $('<dl class="dl-horizontal">');
        details.map(function(item) {
            dl.append(item);
        });
        var tdd = $('<td align="left">');
        tdd.append(dl);
        var tdi = $('<td align="right">');
        tdi.append(createImage(bookJson));
        var tr = $('<tr>');
        tr.append(tdi);
        tr.append(tdd);
        var table = $('<table class="bookDetailPanel">');
        table.append(tr);
        return table;
    }

    function populate() {
        var hostname = location.host;
        var count = 1
        var currentRow = $("<div class='row'>");
        $('.container').append(currentRow);
        $.getJSON("http://" + hostname + "/books", function (data) {
            $.each(data, function (key, val) {
                //var item = $('<li>');
                //item.append(createImage(val));
                if (count % 6 == 0) {
                    currentRow = $('<div class="row">');
                    $('.container').append(currentRow);
                }
                currentRow.append(createImage(val));
                count++;
            });
        });
    };

    // Populate items
    populate();

}(jQuery));