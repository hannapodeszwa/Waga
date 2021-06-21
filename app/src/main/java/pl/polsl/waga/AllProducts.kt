package pl.polsl.waga

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class AllProducts : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_products)


        val exitButton: Button = findViewById(R.id.exitButton)
        exitButton.setOnClickListener {
            createLayoutDynamically(10);
            val myIntent = Intent(this, MainActivity::class.java)
            startActivity(myIntent)
        }
        /////////////////////////////
        createLayoutDynamically(30);
        ///////////////////////////
    }
    private fun createLayoutDynamically(n: Int) {
       // val baseLayout = findViewById<TableLayout>(R.id.table)
        val baseLayout = findViewById<LinearLayout>(R.id.linearLayout)

        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        for (i in 0..n) {
            val layoutH2 = LinearLayout(this)
            baseLayout.addView(layoutH2)

            for(j in 0..2)
            {
                var myButton: ImageButton = ImageButton(this);

                myButton.setImageResource(R.drawable.apple75);
                //myButton.setText(i.toString()+"Button :"+j);
                myButton.setId(i*3 + j);

               // myButton.width = 60
                //myButton.height =50

                myButton.x = 10.0f + j*25.0f
                myButton.y = 10.0f //+ i*25.0f
                var id_ = myButton.getId();

                layoutH2.addView(myButton);
                //baseLayout.addView(myButton);

                myButton.setOnClickListener {
                    Toast.makeText(this,
                        "Button clicked index = " + id_, Toast.LENGTH_SHORT)
                        .show();
                }
            }
        }
    }
}