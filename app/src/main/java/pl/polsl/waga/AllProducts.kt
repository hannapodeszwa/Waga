package pl.polsl.waga

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class AllProducts : AppCompatActivity() {
    private lateinit var labelsList :ArrayList<String>;
    private var imagesList = arrayListOf(R.drawable.apple75, R.drawable.apple75,R.drawable.apple75,R.drawable.apple75,
        R.drawable.apple75, R.drawable.apple75,R.drawable.apple75,R.drawable.apple75,
        R.drawable.apple75, R.drawable.apple75,R.drawable.apple75,R.drawable.apple75,R.drawable.apple75)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_products)

        val exitButton: Button = findViewById(R.id.exitButton)
        exitButton.setOnClickListener {
            val myIntent = Intent(this, MainActivity::class.java)
            startActivity(myIntent)
        }

        var imageIdList = arrayOf<Int>(
            R.drawable.apple75,

            )


        val intent = intent;
        val args = intent.getBundleExtra("BUNDLE")
        labelsList = args!!.getSerializable("labellist") as ArrayList<String>

        createLayoutDynamically();
    }
    private fun createLayoutDynamically() {
        val baseLayout = findViewById<LinearLayout>(R.id.linearLayout)

        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        val buttonsNumber = labelsList.size;

        for (i in 0..buttonsNumber) {
            val layoutH2 = LinearLayout(this)
            baseLayout.addView(layoutH2)

            for(j in 0..2)
            {
                if(i*3+j == buttonsNumber)
                {
                    return
                }

                var myButton: ImageButton = ImageButton(this);

               // myButton.setImageResource(R.drawable.apple75);
                //myButton.setImageResource(imagesList.get((i*3)+j));
                myButton.setImageResource(imagesList.get(0));
                myButton.setId(i*3 + j);

                myButton.x = 10.0f + j*25.0f
                myButton.y = 10.0f
                var id_ = myButton.getId();

                layoutH2.addView(myButton);

                myButton.setOnClickListener {
                  /*  Toast.makeText(this,
                        "Button clicked index = " + id_, Toast.LENGTH_SHORT)
                        .show();*/
                   // val toast = Toast.makeText(applicationContext, "Drukowanie etykiety dla " + labelsList.get(i*3+j) , Toast.LENGTH_SHORT)
                    val toast = Toast.makeText(applicationContext, "Drukowanie etykiety dla " + labelsList.get(0) , Toast.LENGTH_SHORT)

                    toast.show()
                    val myIntent = Intent(this, MainActivity::class.java)
                    startActivity(myIntent)

                }
            }
        }
    }
}