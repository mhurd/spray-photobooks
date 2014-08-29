(function ($) {

    function createImage(bookJson) {
        if (bookJson.smallBookCover != null) {
            return "<a class='thumbnail'><img src='" + bookJson.smallBookCover + "'/></a>";
        } else {
            return "<a class='thumbnail'><img src='../../../assets/img/no-image.jpg'/></a>";
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
        var table = $('<table class="bookDetail bookDetailPanel">');
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
                if (count % 2 == 0) {
                    currentRow = $('<div class="row">');
                    $('.container').append(currentRow);
                }
                    var aDiv = $('<div class="col-lg-6">');
                aDiv.append(createBookDetailPanel(val));
                currentRow.append(aDiv);
                count++;
            });
        });
    };

    // Populate items
    populate();

}(jQuery));