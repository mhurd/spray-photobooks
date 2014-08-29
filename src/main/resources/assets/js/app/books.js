(function ($) {

    function createImage(bookJson) {
        if (bookJson.smallBookCover != null) {
            return "<a class='thumbnail'><img src='" + bookJson.smallBookCover + "'/></a>";
        } else {
            return "<a class='thumbnail'><img src='../../../assets/img/image-not-found.jpg'/></a>";
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
        $.getJSON("http://" + hostname + "/books", function (data) {
            $.each(data, function (key, val) {
                var item = $('<li>');
                item.append(createImage(val));
                $('#items').append(item);
            });
        });
    };

    // Populate items
    populate();

}(jQuery));