# notes-importer
Batch import plain format notes to Apple Notes while retaining original date values

Apple Notes is a native iOS and OS X application that lets you to store your notes on your IMAP account and sync with all devices. However, it lacks the ability to conveniently import your notes from other sources while retaining their original date values. 

I needed to import my 100+ notes from my old phone. So I have digged into how Notes stores those notes on your your IMAP account. It creates a IMAP folder named 'Notes' and saves each note as an email with special header values in that folder. 

I then created a simple utility which reads from an existing directory of plain note files and exports to .eml format, which you can directly drag&drop to your IMAP Notes folder using Thunderbird. 

#### note file format
A file in source folder represents a note, contents of that file represents contents of the note. 

#### datemap file
This file is used to map dates to notes. Each line format: `<date>;<filename>` 
Example file (with date format: yyyyMMdd-HHmmss) 
```
20120101-123000;1.txt
20120106-221101;2.txt
```
Input date format can be overriden by setting `input.date.format` system property. 

If a particular filename cannot be found in datemap file, last modified date of the file will be 
regarded as note date. 

#### running
`mvn compile exec:java -Dexec.args="<sourcefolder> <targetfolder> <datemapfile> <emailaddress>"`

With date format override: 
`mvn compile exec:java -Dinput.date.format="dd.MM.yyyy HH:mm:ss" -Dexec.args="<sourcefolder> <targetfolder> <datemapfile> <emailaddress>"`

Enter email address with which you sync your Notes. 

#### importing to Notes 
After .eml files were generated, you'd use Thunderbird to navigate to Notes directory and drag & drop all .eml files into that directory. When you enter Notes app on your iOS or OS X device, you'll see your imported notes. 

Tested with iOS 8.1.2. 

If you have any questions feel free to ask me. Good luck
