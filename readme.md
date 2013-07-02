# Brewery

**Brewery ist ein Bukkit-Plugin zum brauen auch alkoholischer Getränke.**

Der schwierige Brauprozess belohnt dich mit verschiedensten Getränken, die durch ihre Effekte und Wirkungen eine Trunkenheit hervorrufen können, die es in Minecraft so noch nicht gegeben hat.

## Installation

* Brewery.jar in den Plugins-Ordner kopieren
 
Die Config.yml wird beim Serverstart erstellt.

*Vor der ersten Releaseversion sollte nach jedem Update die Config.yml gelöscht und damit neu generiert werden!*

### Data.yml

Die data.yml beinhaltet alle gespeicherten Daten des Plugins und sollte nicht gelöscht oder verändert werden.

Beim ersten Serverstart wird eine Fehlermeldung darauf hinweisen, dass keine data.yml gefunden wurde. Diese Meldung kann ignoriert werden, da noch keine vorhanden ist und eine neue erstellt wird.

## Config

Die Config.yml enthält:

* Einstellungen
* Rezepte
* Wörter

Sie kann frei editiert werden und nach Belieben angepasst werden. Die meisten Optionen werden allerdings erst bei einem Neustart des Servers übernommen.

Alle Teile der Config sind kommentiert. Die Kommentare enthalten Erklärungen und Beispiele.

Die Bereits vorhandenen Rezepte, Wörter und Einstellungen sind gleichzeitig Beispiele, um z.B. eigene Rezepte zu erstellen.

**Rezepte**

Hier werden die Rezepte für die verschiedenen Tränke eingetragen. Es können bestehende Rezepte verändert, oder neue hinzugefügt werden. Dabei muss beachtet werden, dass alle Zutaten die verwendet werden ebenfalls in der darunterliegenden Kategorie "cooked" eingetragen werden müssen, um sie für den Kessel zugänglich zu machen.

Die Rezepte bestimmen alle Eigenschaften der entstehenden Tränke. Ob und wie lange sie reifen müssen, ihre Farbe, Effekte die beim Trinken wirken, den Alkoholgehalt, usw. Optionen die weggelassen werden, werden nicht verwendet, oder verwenden die Standardwerte. Wenn z.B. die Anzahl der Destilliervorgänge weggelassen wird, muss der Trank nicht destilliert werden.

Wichtig: Werdem die Namen der Rezepte verändert werden vorhandene Tränke der geänderten Rezepte nutzlos!

**Wörter**

Die Wörter sind eine Auflistung von Worten, Sätzen und Buchstaben, die je nach Alkoholisierung des Spielers seinen Chat beeinflussen und dort Wörter verändern. So kann z.B. jedes "a" in ein "b" geändert werden.

Auch hier können anhand der Erklärungen und Beispiele bestehende "Wörter" verändert oder hinzugefügt werden.

## Benutzung

Nach der Installation und Konfiguration kann es ans Brauen von Getränken gehen. Anders als bei Vanilla Minecraft muss hier nicht einfach nur eine Zutat in den Braustand gegeben und gewartet werden. Je nach Rezept kann der Vorgang kompliziert und langwierig sein. Manche Rezepte verlangen eine hohe Genauigkeit in der Einhaltung der Vorgaben. Wird ein Arbeitsschritt zu lange oder zu kurz ausgeführt, kann die Qualität des Getränks stark darunter leiden. Das hat dann zur Folge, dass Nebenwirkungen durch Fuselalkohole und andere Giftstoffe entstehen.

Um ein sinnvolles Brauen der Getränke zu ermöglichen sollten die Spieler die genaue Zusammensetzung der wertvolleren Rezepte nicht kennen (z.B. Rum). Dies steigert den Wert eines hochqualitativen Getränks stark und ein experimentieren und perfektionieren der Getränke wird gefördert.

Je nach Rezept sind einzelne Arbeitsschritte nicht nötig, diese Anleitung beschreibt den allgemeinen Vorgang des Brauens.

### Gähren

Schritt 1 besteht aus dem Gähren/Fermentieren der Zutaten.

1. Cauldron über dem Feuer platzieren
2. Mit Wasser füllen
3. Zutaten hinzugeben
4. Gähren lassen
5. In Glasflaschen abfüllen

### Destillieren

1. Flasche mit Ferment in den Braustand
2. Glowstone als Filter oben in den Braustand (Der Filter wird nicht verbraucht)

### Reifen

Für das reifen ist ein Fass notwendig. Dieses kann auf zwei Arten gebaut werden.

**Kleines Fass**

8 Holztreppen wie folgt anordnen:

![](https://dl.dropboxusercontent.com/u/16240159/Minecraft/Pictures/kl1.png)

Schild "Fass" rechts unten anbringen:

![](https://dl.dropboxusercontent.com/u/16240159/Minecraft/Pictures/kl2.png)

Meldung "Fass wurde erfolgreich erstellt" sollte erscheinen


**Großes Fass**

4 Zäune, 16 Holztreppen, 18 Holzplanken wie folgt anordnen (innen Hohl):

![](https://dl.dropboxusercontent.com/u/16240159/Minecraft/Pictures/gr1.png)

Zapfhahn (Zaun) und Schild "Fass" anbringen:

![](https://dl.dropboxusercontent.com/u/16240159/Minecraft/Pictures/gr2.png)

Meldung "Fass wurde erfolgreich erstellt" sollte erscheinen

------


Das kleine Fass wird mit einem Rechtsklick auf das Schild geöffnet, das Große mit einem Rechtsklick auf den Zapfhahn (Zaun).

Dort hinein kommen nun die Flaschen zum reifen.

Je nach Rezept kann die Art des benutzten Holzes über die Qualität des gereiften Trankes entscheiden

Das Fass sollte während des reifens nicht kaputt gehen, da es sonst ausläuft (nach kurzer Zeit).

Schlägt man den Zapfhahn mit einer Axt ab, so werden alle Getränke herausgeschleudert.

### Trinken

Der Alkoholgehalt des Trankes wird beim trinken auf den Spieler übertragen. Je nach Qualität hat dies verschiedene Auswirkungen.

* Der Spieler kann nicht mehr richtig laufen, er wird Torkeln und für eine Strecke weitaus länger brauchen
* Es können Effekte wie Blindness, Confusion, Poison usw. auftreten
* Der Chat wird stark verändert, vieles was geschrieben wird ist fast unverständlich, teilweise nur Gebrabbel
* Ist der getrunkene Alkohol sehr stark wird er sich leicht vergiften
* Hat er sehr viel getrunken, besteht die Wahrscheinlichkeit, dass er sich übergibt
* Loggt der Spieler sich aus wird er Schwierigkeiten haben sich wieder ein zu loggen, da sein Charakter nicht reagiert
* Trinkt er zu viel wird er in Ohnmacht fallen (Disconnect)

### Ausnüchtern

Nach dem Trinken dauert es eine Weile, bis der Alkohol wieder verschwunden ist. In der Zeit nimmt der Pegel stetig ab.

* Loggt der Spieler sich stark betrunken aus, kann es sein, dass er sich beim nächsten einloggen (wenn etwas Zeit vergangen ist) an einen ihm völlig unbekannten Ort mitten in der Pampa wiederfindet und keine Ahnung hat wie er dort hingkommen ist
* Loggt er sich erst nach einigen Stunden oder am nächsten Morgen wieder ein befindet er sich bei seinem home, ebenfalls ohne jede Erinnerung
* War der Alkohol nicht von der guten Qualität sind mit Nacherscheinungen zu rechnen (Slowness und Hunger).
