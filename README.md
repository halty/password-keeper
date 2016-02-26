# password-keeper
a personal password keeper utility written by pure java supports most OS, like Windows, Linux, OS X etc.

## build
use maven to build the project with follow command:

`mvn install`

## run
find the target 'password-keeper-x.x.x.jar' in which 'x.x.x' is version number, 
then run it on the command line with follow command:

`java -jar password-keeper-x.x.x.jar`

## usage
you can see all the usage info with follow program command:

`help`

the details of all the usage info as follow:
<code>

help -h | -s | cmd
Use examples:
  help -- show all the command documents
  help -s -- show all the command synopsises
  help -h -- show the help info of 'help' command
  help set -- show the 'set' command documents
  help generate key -- show the 'generate key' command documents

list env [-h]
Use examples:
  list env -- list all the customizable program environment variable names
  list env -h -- show the help info of 'list env' command

set -h | (key [value [-p]])
Use examples:
  set -- show all the program environment variables
  set -h -- show the help info of 'set' command
  set keyDir -- show the program environment variable value named with 'keyDir'
  set keyDir /User/key -- set the program environment variable 'keyDir' of value '/User/key'
  set keyDir /User/key -p -- set the program environment variable 'keyDir' of value '/User/key', and store persistently in an implementation-dependent backing store. Each user has a separate user store

generate key -h | (-s keySize -p targetKeyDir)
Note:
  before generate key, you must specify variable named with 'cryptoDriver'
  the generated key depend on specific implementation, default is 'com.lee.password.keeper.impl.crypto.RSACryptoDriver'
Use examples:
  generate key -h -- show the help info of 'generate key' command
  generate key -s 1024 -p '/User/key' -- genearte key with 1024 bits and save them to directory '/User/key'

add web -h | (-k keyword -u url)
Note:
  before add website, you must specify variables 'cryptoDriver' and 'storeDriver'
  default 'cryptoDriver' implementation is 'com.lee.password.keeper.impl.crypto.RSACryptoDriver'
  default 'storeDriver' implementation is 'com.lee.password.keeper.impl.store.BinaryStoreDriver'
Use examples:
  add web -h -- show the help info of 'add web' command
  add web -k amazon -u 'www.amazon.com' -- add 'amazon' website with url 'www.amazon.com'

remove web -h | (-k keyword) | (-i websiteId)
Note:
  while removing a website, you must specify the keyword or websiteId, or both for target removing website
  before remove website, you must specify variables 'cryptoDriver' and 'storeDriver'
  default 'cryptoDriver' implementation is 'com.lee.password.keeper.impl.crypto.RSACryptoDriver'
  default 'storeDriver' implementation is 'com.lee.password.keeper.impl.store.BinaryStoreDriver'
Use examples:
  remove web -h -- show the help info of 'remove web' command
  remove web -k amazon -- remove a website by keyword 'amazon'
  remove web -i 13457927563219 -- remove a website by websiteId '13457927563219'

change web -h | (-k keyword -u url) | (-i websiteId [-k keyword] [-u url])
Note:
  you can change the url by websiteId or keyword, or change the keyword and url by websiteId
  before change website, you must specify variables 'cryptoDriver' and 'storeDriver'
  default 'cryptoDriver' implementation is 'com.lee.password.keeper.impl.crypto.RSACryptoDriver'
  default 'storeDriver' implementation is 'com.lee.password.keeper.impl.store.BinaryStoreDriver'
Use examples:
  change web -h -- show the help info of 'change web' command
  change web -i 13457927563219 -k 'amazon' -- change a website keyword to 'amazon' by websiteId '13457927563219'
  change web -i 13457927563219 -u 'www.amazon.cn' -- change a website url to 'www.amazon.cn' by websiteId '13457927563219'
  change web -i 13457927563219 -k az -u 'www.amazon.cn' -- change a website keyword to 'az' and url to 'www.amazon.cn' by websiteId '13457927563219'
  change web -k amazon -u 'www.amazon.cn' -- change a website url to 'www.amazon.cn' by keyword 'amazon'

query web -h | (-i websiteId) | (-k keyword)
Note:
  you can query the website by websiteId or keyword, or both
  before query website, you must specify variables 'cryptoDriver' and 'storeDriver'
  default 'cryptoDriver' implementation is 'com.lee.password.keeper.impl.crypto.RSACryptoDriver'
  default 'storeDriver' implementation is 'com.lee.password.keeper.impl.store.BinaryStoreDriver'
Use examples:
  query web -h -- show the help info of 'query web' command
  query web -i 13457927563219 -- query the website by websiteId '13457927563219'
  query web -k amazon -- query the website by keyword 'amazon'

count web [-h]
Note:
  before count website, you must specify variables 'cryptoDriver' and 'storeDriver'
  default 'cryptoDriver' implementation is 'com.lee.password.keeper.impl.crypto.RSACryptoDriver'
  default 'storeDriver' implementation is 'com.lee.password.keeper.impl.store.BinaryStoreDriver'
Use examples:
  count web -- count the number of website
  count web -h -- show the help info of 'count web' command

list web [-h]
Note:
  before list website, you must specify variables 'cryptoDriver' and 'storeDriver'
  default 'cryptoDriver' implementation is 'com.lee.password.keeper.impl.crypto.RSACryptoDriver'
  default 'storeDriver' implementation is 'com.lee.password.keeper.impl.store.BinaryStoreDriver'
Use examples:
  list web -- list all the stored websites
  list web -h -- show the help info of 'list web' command

add pwd -h | (((-i websiteId) | (-k websiteKeyword)) -n username -p password [-m memo])
Note:
  before add password, you must specify variables 'cryptoDriver' and 'storeDriver'
  default 'cryptoDriver' implementation is 'com.lee.password.keeper.impl.crypto.RSACryptoDriver'
  default 'storeDriver' implementation is 'com.lee.password.keeper.impl.store.BinaryStoreDriver'
Use examples:
  add pwd -h -- show the help info of 'add pwd' command
  add pwd -i 13457927563219 -n 'peter' -p 123456 -- add username 'peter' and password '123456' to website which id is '13457927563219'
  add pwd -i 13457927563219 -n 'julia' -p 234567 -m 'payCode=love' -- add username 'julia' and password '234567' with memo 'payCode=love' to website which id is '13457927563219'
  add pwd -k 'amazon' -n 'peter' -p 123456 -- add username 'peter' and password '123456' to website which keyword is 'amazon'

remove pwd -h | (((-i websiteId) | (-k websiteKeyword)) -n username)
Note:
  before remove password, you must specify variables 'cryptoDriver' and 'storeDriver'
  default 'cryptoDriver' implementation is 'com.lee.password.keeper.impl.crypto.RSACryptoDriver'
  default 'storeDriver' implementation is 'com.lee.password.keeper.impl.store.BinaryStoreDriver'
Use examples:
  remove pwd -h -- show the help info of 'remove pwd' command
  remove pwd -i 13457927563219 -n 'peter' -- remove password by username 'peter' from website which id is '13457927563219'
  remove pwd -k 'amazon' -n 'peter' -- remove password by username 'peter' from website which keyword is 'amazon'

change pwd -h | (((-i websiteId) | (-k websiteKeyword)) -n username [-p password] [-m memo])
Note:
  before change password, you must specify variables 'cryptoDriver' and 'storeDriver'
  default 'cryptoDriver' implementation is 'com.lee.password.keeper.impl.crypto.RSACryptoDriver'
  default 'storeDriver' implementation is 'com.lee.password.keeper.impl.store.BinaryStoreDriver'
Use examples:
  change pwd -h -- show the help info of 'change pwd' command
  change pwd -i 13457927563219 -n 'peter' -p 987654 -- change password to '987654' by username 'peter' from website which id is '13457927563219'
  change pwd -i 13457927563219 -n 'julia' -m 'payCode=hate' -- change password memo to 'payCode=hate' by username 'julia' from website which id is '13457927563219'
  change pwd -i 13457927563219 -n 'julia' -p 876543 -m 'payCode=hate' -- change password to '987654' and memo to 'payCode=hate' by username 'julia' from website which id is '13457927563219'
  change pwd -k 'amazon' -n 'peter' -p 987654 -- change password to '987654' by username 'peter' from website which keyword is 'amazon'

query pwd -h | (((-i websiteId) | (-k websiteKeyword)) -n username)
Note:
  before query password, you must specify variables 'cryptoDriver' and 'storeDriver'
  default 'cryptoDriver' implementation is 'com.lee.password.keeper.impl.crypto.RSACryptoDriver'
  default 'storeDriver' implementation is 'com.lee.password.keeper.impl.store.BinaryStoreDriver'
Use examples:
  query pwd -h -- show the help info of 'query pwd' command
  query pwd -i 13457927563219 -n 'peter' -- query password by username 'peter' from website which id is '13457927563219'
  query pwd -k 'amazon' -n 'peter' -- query password by username 'peter' from website which keyword is 'amazon'

count pwd -h | ([(-i websiteId) | (-k websiteKeyword)] [-n username])
Note:
  before count password, you must specify variables 'cryptoDriver' and 'storeDriver'
  default 'cryptoDriver' implementation is 'com.lee.password.keeper.impl.crypto.RSACryptoDriver'
  default 'storeDriver' implementation is 'com.lee.password.keeper.impl.store.BinaryStoreDriver'
Use examples:
  count pwd -- count the total number of password
  count pwd -h -- show the help info of 'count pwd' command
  count pwd -i 13457927563219 -- count the number of password from website which id is '13457927563219'
  count pwd -n 'peter' -- count the number of password by username 'peter'
  count pwd -i 13457927563219 -n 'peter' -- count the number of password by username 'peter' from website which id is '13457927563219'
  count pwd -k 'amazon' -n 'peter' -- count the number of password by username 'peter' from website which keyword is 'amazon'

list pwd -h | (((-i websiteId) | (-k websiteKeyword)) [-n username]) | (-n username)
Note:
  before list password, you must specify variables 'cryptoDriver' and 'storeDriver'
  default 'cryptoDriver' implementation is 'com.lee.password.keeper.impl.crypto.RSACryptoDriver'
  default 'storeDriver' implementation is 'com.lee.password.keeper.impl.store.BinaryStoreDriver'
Use examples:
  list pwd -h -- show the help info of 'list pwd' command
  list pwd -i 13457927563219 -- list passwords from website which id is '13457927563219'
  list pwd -n 'peter' -- list passwords by username 'peter'
  list pwd -i 13457927563219 -n 'peter' -- list passwords by username 'peter' from website which id is '13457927563219'
  list pwd -k 'amazon' -n 'peter' -- list passwords by username 'peter' from website which keyword is 'amazon'

undo [-h]
Note:
  before undo, you must specify variables 'cryptoDriver' and 'storeDriver'
  default 'cryptoDriver' implementation is 'com.lee.password.keeper.impl.crypto.RSACryptoDriver'
  default 'storeDriver' implementation is 'com.lee.password.keeper.impl.store.BinaryStoreDriver'
Use examples:
  undo -- undo the last uncommitted change operation
  undo -h -- show the help info of 'undo' command

redo [-h]
Note:
  before redo, you must specify variables 'cryptoDriver' and 'storeDriver'
  default 'cryptoDriver' implementation is 'com.lee.password.keeper.impl.crypto.RSACryptoDriver'
  default 'storeDriver' implementation is 'com.lee.password.keeper.impl.store.BinaryStoreDriver'
Use examples:
  redo -- redo the last undo change operation
  redo -h -- show the help info of 'redo' command

commit [-h]
Note:
  before commit, you must specify variables 'cryptoDriver' and 'storeDriver'
  default 'cryptoDriver' implementation is 'com.lee.password.keeper.impl.crypto.RSACryptoDriver'
  default 'storeDriver' implementation is 'com.lee.password.keeper.impl.store.BinaryStoreDriver'
Use examples:
  commit -- commit all the change operation since the last commit operation, if commit success, all the change will be flush to underlying storage, and can not be undo by 'undo' command
  commit -h -- show the help info of 'commit' command

exit [-h]
Use examples:
  exit -- releases any associated system resources and exit
  exit -h -- show the help info of 'exit' command

</code>