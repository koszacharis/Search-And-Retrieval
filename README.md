## Search-And-Retrieval

####1) Create a Spatial Index in MySQL
Create a new table that associates geo-coordinates (as POINTs) to item-IDs, fill this table with all items that have latitude and longitude information, create a spatial index for the point column of your table from the previous step.
####2) Create Lucene Index
Write a program Indexer.java that creates a lucene index. The index should be stored in a directory named indexes (under the current working directory from which runLoad.sh is called). We will want to use this index to carry out keyword searches over the union of the name, categories, and description of an item.
####3) Implement Search function
Write the Java program Searcher.java which carries out keyword and spatial search over the ebay data. The Searcher program will take as first argument a list of space-separated keywords, given within quotes. For instance, java Searcher "star trek"
returns a list item-IDs, item-names, and Lucene scores of all items that contain the word "star" or the word "trek" (or both) in the name of the item, or in one of the categories of the item, or in the description of the item.

Searcher program should be implemented in such a way that it can also take three arguments, in the following way:
java Searcher "star trek" -x longitude -y latitude -w width
where longitude, latitude, and width are numbers. The longitude and latitude numbers descibe a geo-location, and the width is a number that describes the width of a square, in kilometers. If such three parameters are given, then your program should further restrict the results of the keyword search by only returning items that have longitude and latitude information, and for which these numbers fall into the "square" that is centered at the given longitude and latitude numbers, and that has width given by the width number. Return the items in this way: first the item-ID, then the name, then the Lucene score, and then the distance from the given geo-location (in kilometers).

###Ranking
If Searcher program is called only with the keywords and without the other parameters, then the ranked list of item-IDs, names, and scores should be ordered by decreasing Lucene-score for items with equal Lucene score, order them by increasing price (i.e., lowest price first). Here, price is defined as the current price of the item.

If Searcher program is called with the latitude, longitude, and width parameters, then the ranked list of item-IDs and names of items should be ordered by decreasing Lucene-score for items with equal Lucene score, order them by increasing distance from the geo-location given by the latitude and longitude numbers (i.e., smallest distance first = closest items first)
for items with equal Lucene score and equal distance, order them by increasing price (i.e., lowest price first), where price is defined as before.
